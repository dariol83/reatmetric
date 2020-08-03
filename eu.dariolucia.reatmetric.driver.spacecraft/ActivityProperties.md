## Activity Properties

### Packet level

#### pus-ack-override 

Override the PUS ack flags specified in the packet definition. 

Format: `[X|-][X|-][X|-][X|-]`

Example: `X-XX`

#### pus-source-override 

Override the PUS source ID specified in the packet definition or configuration. 

Format: `[0-9]+`

Example: `14`

#### map-id-override 

Override the Map ID specified in the packet definition. 

Format: `[0-9]+`

Example: `2`

#### tc-scheduled-time

Specify the on-board execution time of the telecommand. If this property is specified, the encoded telecommand will be
wrapped into the configured PUS 11,4 command.

Format:  `ISO-8601 instant format`

Example: `2011-12-23T10:15:30Z`

#### tc-vc-id-override 

Override the TC VC ID specified in the configuration for generated TC frames. 

Format: `[0-7]`

Example: `1`

#### use-ad-mode-override 

Override the currently specified TC frame transfer mode for generated TC frames.
If set to 'true', the TC frame will have the bypass flag unset. 

Format: `true|false`

Example: `true`

#### group-tc-name 

Inform the TC Data Link processor that the TC packet is part of a group and shall
be encoded inside a single frame with other commands. The string set here identifies
the name of the group. 

Format: `[0-9a-zA-Z]+'`

Example: `Group1`

#### group-tc-transmit

Inform the TC Data Link processor that the TC packet is the last one of the group
identified with the group-tc-name property. The group is closed, encoded and the resulting
frame transmitted.

Format: `true|false`
 
Example: `true`

#### onboard-sub-schedule-id

Override the sub-schedule ID for telecommands wrapped into a PUS 11,4 packet.

Format: `[0-9]+`

Example: `2`

#### linked-scheduled-activity-occurrence

Allow to keep tracking between a scheduled activity occurrence and the PUS 11,4 TC.
This implementation of the PUS 11 supports a single TC per PUS 11,4.

Format: `internal`
