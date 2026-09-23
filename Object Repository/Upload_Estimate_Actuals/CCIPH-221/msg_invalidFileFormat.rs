<?xml version="1.0" encoding="UTF-8"?>
<WebElementEntity>
   <description>Invalid file format validation (AC2/CCIPH-75)</description>
   <name>msg_invalidFileFormat</name>
   <tag></tag>
   <elementGuidId>uea-cciph221-msg-invalid-format</elementGuidId>
   <selectorCollection>
      <entry>
         <key>XPATH</key>
         <value>//div[@role='alert' and contains(@class,'fileError')] | //*[contains(@class,'dropZoneExportRow')]//div[@role='alert'] | //div[@role='alert' and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'invalid file format')]</value>
      </entry>
   </selectorCollection>
   <selectorMethod>XPATH</selectorMethod>
   <smartLocatorEnabled>true</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
</WebElementEntity>
