This plugin synchronizes time tracking information from JIRA to Vertec.

Installation
------------
1. Upload the jar file to JIRA
2. Create the custom field and add it to your issues. Under 'Advanced' search for 'Vertec Type' (see https://confluence.atlassian.com/display/JIRA/Adding+a+Custom+Field)
3. Setup connection to Vertec. Under Administration -> Add-ons -> Integrations -> Vertec Konfiguration set URL and a user/password. After a successful synchronization, the dropdown field contains your Vertec projects.
4. Set the custom field value on your issues. The value will be inherited from epics to user stories and issues. So the best way is to set the value on your epic.
   To break inheritance override the value with a different project/phase.

Now you're ready - happy time tracking


Notes
-----
- If the synchronization to Vertec fails, JIRA's time tracking information will also be deleted. The JIRA user will be informed by email.
- Be aware of Vertec's 'Sperrperiode'! Each booking within this period will be rejected. These bookings remain also in the session of your Vertec user. You need to login and logout with this user to remove them.

 
