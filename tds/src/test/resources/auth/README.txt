Keystore was generated with:

$ keytool -genkey -alias tds -keyalg RSA -validity 3650 -keystore keystore
Enter keystore password: secret666
Re-enter new password: secret666
What is your first and last name?
  [Unknown]:  localhost
What is the name of your organizational unit?
  [Unknown]:
What is the name of your organization?
  [Unknown]:
What is the name of your City or Locality?
  [Unknown]:
What is the name of your State or Province?
  [Unknown]:
What is the two-letter country code for this unit?
  [Unknown]:
Is CN=localhost, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown correct?
  [no]:  yes

Enter key password for <tds>
	(RETURN if same as keystore password): RETURN


In tomcat-users.xml, we have:

  <user username="tds"
        password="secret666"
        roles="tdsConfig,manager-gui,tdsMonitor"/>

We're using the "tds" alias and its "secret666" password that we setup in the keystore. The "tds" user has
"tdsConfig", "manager-gui", and "tdsMonitor" powers.
