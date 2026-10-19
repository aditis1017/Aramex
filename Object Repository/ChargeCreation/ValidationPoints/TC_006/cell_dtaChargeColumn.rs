<?xml version="1.0" encoding="UTF-8"?>
<WebElementEntity>
   <description>DTA charge row cell by description + column header (reference locator — built dynamically in keywords)</description>
   <name>cell_dtaChargeColumn</name>
   <tag></tag>
   <elementGuidId>cc-vp-tc006-cell-dta-charge-0001</elementGuidId>
   <selectorCollection>
      <entry>
         <key>XPATH</key>
         <value>//table[contains(@class,'chargesTable')][.//th[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'eligible cost')]]//tbody/tr[contains(@class,'chargesDataRow')][td[contains(@class,'chargesCellDescription') and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'import vat')]]/td[count(ancestor::table[1]//thead//th[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'ar currency')][1]/preceding-sibling::th)+1]</value>
      </entry>
   </selectorCollection>
   <selectorMethod>XPATH</selectorMethod>
   <smartLocatorEnabled>true</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
</WebElementEntity>
