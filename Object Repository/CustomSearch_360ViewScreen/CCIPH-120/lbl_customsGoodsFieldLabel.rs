<?xml version="1.0" encoding="UTF-8"?>
<WebElementEntity>
   <description>Field label under Customs &amp; Goods on Charge Detail. Replace ${fieldLabel} with exact label (e.g. Insurance Value, Shipment Business Type).</description>
   <name>lbl_customsGoodsFieldLabel</name>
   <tag></tag>
   <elementGuidId>cs-cciph120-lbl-customs-goods-field-0001</elementGuidId>
   <selectorCollection>
      <entry>
         <key>XPATH</key>
         <value>//span[starts-with(@class,'CustomsChargeDetail-module__GjzTcG__') and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'customs') and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'goods')]/ancestor::div[contains(@class,'CustomsChargeDetail-module__GjzTcG__')][1]//p[starts-with(@class,'CustomsChargeDetail-module__GjzTcG__fieldLabel') and normalize-space(.)='${fieldLabel}']</value>
      </entry>
   </selectorCollection>
   <selectorMethod>XPATH</selectorMethod>
   <smartLocatorEnabled>true</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
</WebElementEntity>
