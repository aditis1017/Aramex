<?xml version="1.0" encoding="UTF-8"?>
<WebElementEntity>
   <description>Charge detail field value scoped to additional-details section (e.g. origin + Entity). Variable sectionName: lowercase alphanumeric section suffix (origin, destination, goodssummary). Variable fieldName: exact field label.</description>
   <name>val_sectionFieldValue</name>
   <tag></tag>
   <elementGuidId>cc-vp-tc007-val-section-field-0001</elementGuidId>
   <selectorCollection>
      <entry>
         <key>XPATH</key>
         <value>//div[contains(@class,'CustomsChargeDetail-module__GjzTcG__additionalSection') and contains(translate(@class,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),concat('additionalsection','${sectionName}'))]//p[starts-with(@class,'CustomsChargeDetail-module__GjzTcG__fieldLabel') and normalize-space(.)='${fieldName}']/following-sibling::div[starts-with(@class,'CustomsChargeDetail-module__GjzTcG__fieldValue')][1]</value>
      </entry>
   </selectorCollection>
   <selectorMethod>XPATH</selectorMethod>
   <smartLocatorEnabled>true</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
</WebElementEntity>
