<?xml version="1.0" encoding="UTF-8"?>
<WebElementEntity>
   <description>Oversized file validation alert for B2B upload above 35 MB (TC_B2B_012)</description>
   <name>msg_fileTooLarge</name>
   <tag></tag>
   <elementGuidId>b2b-cciph226-0019</elementGuidId>
   <selectorCollection>
      <entry>
         <key>XPATH</key>
         <value>//div[@role='alert' and contains(@class,'fileError') and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'file is too large')] | //*[contains(@class,'dropZoneExportRow')]//div[@role='alert' and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'maximum size is 35 mb')]</value>
      </entry>
   </selectorCollection>
   <selectorMethod>XPATH</selectorMethod>
   <smartLocatorEnabled>false</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
</WebElementEntity>
