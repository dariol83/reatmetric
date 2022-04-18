/*
 * Copyright (c)  2021 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.driver.serial.protocol;

import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.api.value.ValueUtil;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProtocolManager {

    private static final Logger LOG = Logger.getLogger(ProtocolManager.class.getName());

    private static final String C_HELLO =               "HELLO";
    private static final String C_BYE =                 "BYE";
    private static final String C_PING =                "PING";
    private static final String C_SET_VALUE_LEN =       "SET_VALUE_LEN";
    private static final String C_REG_PARAM =           "REG_PARAM";
    private static final String C_DEREG_PARAM =         "DEREG_PARAM";
    private static final String C_DEREG_PARAM_ALL =     "DEREG_PARAM_ALL";
    private static final String C_UPDATE_PARAM =        "UPDATE_PARAM";
    private static final String C_SET_MAX_LOG =         "SET_MAX_LOG";
    private static final String C_SET_LOG_LEN =         "SET_LOG_LEN";
    private static final String C_UPDATE_LOG =          "UPDATE_LOG";

    private static final String EOL = "" + (char) 13;
    private static final byte[] S_ABORT =   "ABORT".concat(EOL).getBytes(StandardCharsets.US_ASCII);
    private static final byte[] S_HI =      "HI RTM".concat(EOL).getBytes(StandardCharsets.US_ASCII);
    private static final byte[] S_CYA =     "CYA".concat(EOL).getBytes(StandardCharsets.US_ASCII);
    private static final byte[] S_PONG =    "PONG".concat(EOL).getBytes(StandardCharsets.US_ASCII);
    private static final byte[] S_OK =      "OK".concat(EOL).getBytes(StandardCharsets.US_ASCII);
    private static final byte[] S_KO =      "KO".concat(EOL).getBytes(StandardCharsets.US_ASCII);

    private int valueLength =   15;
    private int maxLogs =       4;
    private int logLength =     26;
    private final int timeoutSeconds;

    private enum ProtocolState {
        DEREGISTERED,
        REGISTERED
    }

    private ProtocolState state = ProtocolState.DEREGISTERED;

    private String currentClient;

    private final Timer timer = new Timer("Reatmetric Serial Driver - Timeout Timer");
    private TimerTask currentRunningTimer = null;

    private final IMonitoringDataManager externalManager;

    private final Map<String, Integer> path2id = new TreeMap<>();

    public ProtocolManager(IMonitoringDataManager externalManager, int timeoutSeconds) {
        this.externalManager = externalManager;
        this.timeoutSeconds = timeoutSeconds;
    }

    public synchronized byte[] event(String clientMessage) {
        restartReceptionTimer();
        // Remove the CRLF
        String trimmedMessage = clientMessage.trim();
        // Split on space
        String[] parts = trimmedMessage.split(" ", -1);
        // Process message depending on state
        switch (state) {
            case DEREGISTERED:
                return processOnDeregistered(trimmedMessage, parts);
            case REGISTERED:
                return processOnRegistered(trimmedMessage, parts);
            default:
                throw new IllegalStateException("Error: state not supported: " + state);
        }
    }

    private byte[] processOnDeregistered(String fullMessage, String[] parts) {
        // Guard condition
        if(parts == null || parts.length == 0 || parts[0] == null) {
            // Stay on DEREGISTERED, send ABORT
            return S_ABORT;
        }
        switch (parts[0]) {
            case C_HELLO: {
                if(parts.length <= 1) {
                    // Stay on DEREGISTERED, send ABORT
                    return S_ABORT;
                }
                // Move to REGISTERED
                switchStateTo(ProtocolState.REGISTERED);
                // Get the name
                currentClient = fullMessage.substring(parts[0].length() + 1);
                // Return response
                return S_HI;
            }
            default: {
                // Stay on DEREGISTERED, send ABORT
                return S_ABORT;
            }
        }
    }

    private byte[] processOnRegistered(String fullMessage, String[] parts) {
        // Guard condition
        if(parts == null || parts.length == 0 || parts[0] == null) {
            // Move to DEREGISTERED, send ABORT
            return abort();
        }
        switch (parts[0]) {
            case C_BYE:
                return bye();
            case C_PING:
                return ping();
            case C_SET_VALUE_LEN:
                return setValueLength(parts);
            case C_SET_MAX_LOG:
                return setMaxLogs(parts);
            case C_SET_LOG_LEN:
                return setLogLength(parts);
            case C_REG_PARAM:
                return registerParameter(parts);
            case C_DEREG_PARAM:
                return deregisterParameter(parts);
            case C_DEREG_PARAM_ALL:
                return deregisterAllParameters();
            case C_UPDATE_LOG:
                return updateLog();
            case C_UPDATE_PARAM:
                return updateParameters();
            default:
                return abort();
        }
    }

    private byte[] abort() {
        // Move to DEREGISTERED, send ABORT
        switchStateTo(ProtocolState.DEREGISTERED);
        return S_ABORT;
    }

    private byte[] updateParameters() {
        List<ParameterData> params = this.externalManager.updateParameters();
        StringBuilder sb = new StringBuilder();
        // Number of records
        sb.append(String.format("%02d%s", params.size(), EOL));
        // Iterate, find ID from local map, construct string
        for(ParameterData pd : params) {
            int id = this.path2id.get(pd.getPath().asString());
            String paramStr = generateParameterString(id, pd);
            sb.append(paramStr);
        }
        sb.append(String.format("OK%s", EOL));
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private String generateParameterString(int id, ParameterData parameter) {
        GregorianCalendar d = new GregorianCalendar();
        d.setTimeZone(TimeZone.getTimeZone("UTC"));
        d.setTime(new Date(parameter.getGenerationTime().toEpochMilli()));
        String toReturn = String.format("%02d %02d:%02d:%02d %s %s %s", id, d.get(Calendar.HOUR_OF_DAY), d.get(Calendar.MINUTE), d.get(Calendar.SECOND),
                getParameterValue(parameter.getEngValue()),
                getParameterValidity(parameter.getValidity()),
                getParameterAlarm(parameter.getAlarmState()));
        return toReturn.concat(EOL);
    }

    private String getParameterValue(Object engValue) {
        String valStr = "<null>";
        if(engValue != null) {
            valStr = ValueUtil.toString(engValue);
        }
        if(valStr.length() < this.valueLength) {
            valStr = String.format("%1$" + this.valueLength + "s", valStr);
        } else {
            valStr = valStr.substring(0, this.valueLength);
        }
        return valStr;
    }

    private String getParameterAlarm(AlarmState alarmState) {
        switch(alarmState) {
            case ALARM: return "ALM";
            case WARNING: return "WRN";
            case VIOLATED: return "VIO";
            case ERROR: return "ERR";
            case NOT_CHECKED: return "N/C";
            case NOMINAL: return "NOM";
            case NOT_APPLICABLE: return "N/A";
            case IGNORED: return "IGN";
            default: return "UNK";
        }
    }

    private String getParameterValidity(Validity validity) {
        switch (validity) {
            case VALID: return "V";
            case INVALID: return "I";
            case ERROR: return "E";
            case DISABLED: return "D";
            default: return "U";
        }
    }

    private byte[] updateLog() {
        List<OperationalMessage> logs = this.externalManager.updateLogs();
        // Assuming that the logs are orderer in generation time ascending, retain the last this.maxLogs logs
        if(logs.size() > this.maxLogs) {
            logs = logs.subList(logs.size() - this.maxLogs, logs.size());
        }
        StringBuilder sb = new StringBuilder();
        // Number of records
        sb.append(String.format("%02d%s", logs.size(), EOL));
        // Iterate, construct string
        for(OperationalMessage pd : logs) {
            GregorianCalendar d = new GregorianCalendar();
            d.setTimeZone(TimeZone.getTimeZone("UTC"));
            d.setTime(new Date(pd.getGenerationTime().toEpochMilli()));
            String logStr = String.format("%02d:%02d:%02d %s %s", d.get(Calendar.HOUR_OF_DAY), d.get(Calendar.MINUTE), d.get(Calendar.SECOND),
                    getLogSeverity(pd.getSeverity()),
                    getLogMessage(pd.getMessage()));
            sb.append(logStr).append(EOL);
        }
        sb.append(String.format("OK%s", EOL));
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private String getLogMessage(String message) {
        if(message.length() > this.logLength) {
            return message.substring(0, this.logLength);
        } else {
            return String.format("%1$" + this.logLength + "s", message);
        }
    }

    private String getLogSeverity(Severity severity) {
        switch(severity) {
            case ALARM: return "ALM";
            case WARN: return "WRN";
            case INFO: return "INF";
            case ERROR: return "ERR";
            case CHAT: return "CHT";
            case NONE: return "NON";
            default: return "UNK";
        }
    }

    private byte[] ping() {
        // Restart timer
        restartReceptionTimer();
        // Stay on REGISTERED, send PONG
        return S_PONG;
    }

    private byte[] bye() {
        // Move to DEREGISTERED, send CYA
        switchStateTo(ProtocolState.DEREGISTERED);
        return S_CYA;
    }

    private byte[] deregisterAllParameters() {
        this.externalManager.deregisterAllParameter();
        // Remove all correspondences from local map
        this.path2id.clear();
        // Stay on REGISTERED, send OK
        return S_OK;
    }

    private byte[] registerParameter(String[] parts) {
        // Delegate to the external manager
        int id = this.externalManager.registerParameter(parts[1]);
        if(id < 0) {
            // Stay on REGISTERED, send KO
            return S_KO;
        } else {
            // Keep correspondence into a local map
            this.path2id.put(parts[1], id);
            // Stay on REGISTERED, send OK and parameter ID
            return String.format("OK %02d%s", id, EOL).getBytes(StandardCharsets.US_ASCII);
        }
    }

    private byte[] deregisterParameter(String[] parts) {
        // Try to convert the parameter ID
        try {
            int id = Integer.parseInt(parts[1]);
            boolean deregistered = this.externalManager.deregisterParameter(id);
            if(deregistered) {
                // Remove correspondence from local map
                String path = null;
                for(Map.Entry<String, Integer> e : this.path2id.entrySet()) {
                    if(e.getValue() == id) {
                        path = e.getKey();
                        break;
                    }
                }
                if(path != null) {
                    this.path2id.remove(path);
                }
                // Stay on REGISTERED, send OK
                return S_OK;
            } else {
                // Stay on REGISTERED, send KO
                return S_KO;
            }
        } catch (Exception e) {
            // Stay on REGISTERED, send KO
            return S_KO;
        }
    }

    private byte[] setMaxLogs(String[] parts) {
        // Try to get the length of the value
        try {
            this.maxLogs = Integer.parseInt(parts[1]);
            // Stay on REGISTERED, send OK
            return S_OK;
        } catch (Exception e) {
            // Stay on REGISTERED, send KO
            return S_KO;
        }
    }

    private byte[] setLogLength(String[] parts) {
        // Try to get the length of the value
        try {
            this.logLength = Integer.parseInt(parts[1]);
            // Stay on REGISTERED, send OK
            return S_OK;
        } catch (Exception e) {
            // Stay on REGISTERED, send KO
            return S_KO;
        }
    }

    private byte[] setValueLength(String[] parts) {
        // Try to get the length of the value
        try {
            this.valueLength = Integer.parseInt(parts[1]);
            // Stay on REGISTERED, send OK
            return S_OK;
        } catch (Exception e) {
            // Stay on REGISTERED, send KO
            return S_KO;
        }
    }

    private void restartReceptionTimer() {
        stopReceptionTimer();
        this.currentRunningTimer = new TimerTask() {
            @Override
            public void run() {
                receptionTimeoutElapsed(this);
            }
        };
        this.timer.schedule(this.currentRunningTimer, timeoutSeconds * 1000);
    }

    private synchronized void receptionTimeoutElapsed(TimerTask timerTask) {
        if(this.currentRunningTimer == timerTask) {
            switchStateTo(ProtocolState.DEREGISTERED);
        }
    }

    private void stopReceptionTimer() {
        if(this.currentRunningTimer != null) {
            this.currentRunningTimer.cancel();
            this.currentRunningTimer = null;
        }
    }

    private void switchStateTo(ProtocolState newState) {
        this.state = newState;
        if(this.state == ProtocolState.DEREGISTERED) {
            this.externalManager.deregisterAllParameter();
            this.path2id.clear();
            stopReceptionTimer();
        } else if(this.state == ProtocolState.REGISTERED) {
            restartReceptionTimer();
        }
        LOG.log(Level.INFO, "Serial protocol manager state switched to " + newState);
    }

    public synchronized boolean isRegistered() {
        return state == ProtocolState.REGISTERED;
    }

    public synchronized String getCurrentClient() {
        return currentClient;
    }

    public synchronized void dispose() {
        switchStateTo(ProtocolState.DEREGISTERED);
    }
}
