<?xml version="1.0" encoding="UTF-8"?>

<WebElementEntity>

   <description>DTA billing account value (ASL001) on charge detail</description>

   <name>val_billingAccount</name>

   <tag></tag>

   <elementGuidId>cc77-val-billing-account-0001</elementGuidId>

   <selectorCollection>

      <entry>

         <key>XPATH</key>

         <value>//*[contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'BILLING ACCOUNT') or contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'ACCOUNT')]/following::*[self::td or self::span or self::div][contains(normalize-space(.),'ASL') or normalize-space()!=''][1]</value>

      </entry>

   </selectorCollection>

   <selectorMethod>XPATH</selectorMethod>

   <smartLocatorEnabled>true</smartLocatorEnabled>

   <useRalativeImagePath>false</useRalativeImagePath>

</WebElementEntity>

