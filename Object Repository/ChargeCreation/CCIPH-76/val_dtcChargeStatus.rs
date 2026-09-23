<?xml version="1.0" encoding="UTF-8"?>
<WebElementEntity>
   <description>DTC charge status badge (Due / Settled)</description>
   <name>val_dtcChargeStatus</name>
   <tag></tag>
   <elementGuidId>cc76-val-dtc-status-0001</elementGuidId>
   <selectorCollection>
      <entry>
         <key>XPATH</key>
         <value>//*[contains(@class,'badge') or contains(@class,'status') or contains(@class,'pill')][contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'DUE') or contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'SETTLED')]</value>
      </entry>
   </selectorCollection>
   <selectorMethod>XPATH</selectorMethod>
   <smartLocatorEnabled>true</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
</WebElementEntity>
