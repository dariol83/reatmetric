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