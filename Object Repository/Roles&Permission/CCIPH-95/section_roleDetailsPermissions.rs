<?xml version="1.0" encoding="UTF-8"?>
<WebElementEntity>
   <description>Permissions section grouped by module on role details screen</description>
   <name>section_roleDetailsPermissions</name>
   <tag></tag>
   <elementGuidId>urm-cciph95-0002</elementGuidId>
   <selectorCollection>
      <entry>
         <key>XPATH</key>
         <value>//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'permissions') and not(contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'edit permission'))]/ancestor::*[self::section or self::div][1] | //*[contains(@class,'permission')] | //*[@role='dialog' or contains(@class,'modal') or contains(@class,'dialog') or contains(@class,'drawer') or contains(@class,'Drawer')]//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'permission')]</value>
      </entry>
   </selectorCollection>
   <selectorMethod>XPATH</selectorMethod>
   <smartLocatorEnabled>false</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
</WebElementEntity>
