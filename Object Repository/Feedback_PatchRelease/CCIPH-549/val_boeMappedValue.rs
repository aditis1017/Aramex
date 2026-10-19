<?xml version="1.0" encoding="UTF-8"?>
<WebElementEntity>
   <description>Declaration Number mapped under a BOE slot on 360 View. Variables: boeSlot (boe1/boe2/boe3/additional), declarationNumber</description>
   <name>val_boeMappedValue</name>
   <tag></tag>
   <elementGuidId>fpr-cciph549-val-boe-mapped-0001</elementGuidId>
   <selectorCollection>
      <entry>
         <key>XPATH</key>
         <value>//table[.//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'service code') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'actual one')]]//*[self::th or self::td or self::div or self::span][(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='${boeSlot}' or starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'${boeSlot}')) and contains(normalize-space(.),'${declarationNumber}')]</value>
      </entry>
   </selectorCollection>
   <selectorMethod>XPATH</selectorMethod>
   <smartLocatorEnabled>true</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
</WebElementEntity>
