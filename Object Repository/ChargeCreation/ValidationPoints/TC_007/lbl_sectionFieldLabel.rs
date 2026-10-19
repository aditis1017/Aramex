<?xml version="1.0" encoding="UTF-8"?>
<WebElementEntity>
   <description>Charge detail field label (text only) scoped to an additional-details section. sectionName: lowercase alphanumeric suffix (e.g. customsgoods, origin). fieldName: exact visible label.</description>
   <name>lbl_sectionFieldLabel</name>
   <tag></tag>
   <elementGuidId>cc-vp-tc007-lbl-section-field-0001</elementGuidId>
   <selectorCollection>
      <entry>
         <key>XPATH</key>
         <value>//div[contains(@class,'CustomsChargeDetail-module__GjzTcG__additionalSection') and contains(translate(@class,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),concat('additionalsection','${sectionName}'))]//p[starts-with(@class,'CustomsChargeDetail-module__GjzTcG__fieldLabel') and normalize-space(.)='${fieldName}']</value>
      </entry>
   </selectorCollection>
   <selectorMethod>XPATH</selectorMethod>
   <smartLocatorEnabled>true</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
</WebElementEntity>
