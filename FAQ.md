### Frequently Asked Questions ###  

  + #### What is Notifier/Notifications center  ####
    Notifier/Notifications center, tagged 'Better notifications for Android', is a mailbox for your status bar notifications.  
    The app aims to unclutter your phone's status bar by organizing incoming notifications akin to a mailbox, grouping them by their respective apps for easy access.  
    Notifier also puts search at the forefront of every view so notification(s) of interest can be looked up quickly using keyword(s).

  + #### How does the search work? ####
    keyword-based fuzzy search with instant search results.  
    Search starts right after you start typing, refining results as more characters are being typed.  
    ##### Search semantics:  ##### 
      1. Search key: abc def  
         Equivalent to abc *AND* def, returns all notifications with both abc and def across each notification's application name, title and content.  
         So a search for `twi bit` can return all twitter notifications with 'bit' in their content.   
      
      2. Search key: `"abc def"` returns all notifications matching the exact phrase.  

    ##### A few examples: #####  
       Search key: `car key` matches all the following texts  
```
   1. Key drivers in Nascar championships   
   2. Lost my car key today.  
   3. Where is my keycard?  
   4. Discarded bottles of whiskey.  
```
   In contrast, Search key: `"car key"` will match only one of the aforementioned texts.  
```
   2. Lost my car key today.
``` 
        
          
  + #### What are 'Cached notifications' ####
    Notifier caches notifications previously cleared from the status bar for an user-configured interval of time.  
    To distinguish these cleared notifications from the ones currently active in the status bar, the app labels them as 'Cached Notifications'.  

  + #### Is this a replacement for status bar? #### 
    NO, Notifier is intended to work with the status bar notifications as a supplement.  
    While it is possible to use notifier as a replacement (See: 'auto-remove status bar notifications' in app settings), the app doesn't have all the functions the status bar does, and can lose all stored notifications in the event of a crash or a restart [The app runs entirely in system memory (RAM)].  
    
  + #### How do I expand/collapse notifications? ####
    The functional equivalent of swiping down/up to expand/collapse status bar notifications respectively, can be performed by clicking on the top row containing the notification icon, name and time to expand/collapse notifications.  

  + #### Privacy and security ####
    + ##### How secure is my data? #####
      Notifier runs entirely in memory, leaves no footprints either on the phone storage nor does it connect to the internet.  
      Without any persistent storage or network access, avenues to communicate any data that might be collected are very limited.  

    + ##### Can I trust the apk to be in sync with the source? #####
      NO. It's a good policy to TNO (Trust No One).  
      Notifier is open-source - Anyone can add harmful code to it and distribute the malicious binary apk.  
      When in doubt, build fresh from the source.  
      
  + #### Troubleshooting ####
    + ##### Notifier takes too much memory #####
      Reduce the caching timeout to reduce the memory footprint of the app.  
      Alternately, Clear un-needed notifications from the cached list by swiping them to reclaim memory held by them.  
    	If the aforementioned steps don't help, Navigate to android settings -> apps -> Notifier, and force-stop.  
	    Force-stopping restarts the notifier app and re-initializes from the status bar, flushing all previous data.  

    + ##### Why don't I see some of the notifications in the status bar, but are missing from the notifications center? #####
      Notifications Center ignores persistent notifications (the ones that cannot be swiped away), and android system notifications (includes keyboard-change, hotspot-on messages, et, al) by default, and won't appear in the app unless configured to do so.  
      [See Settings -> Show persistent notifications/ Show System notifications]  

    + ##### Why do some notifications appear incomplete? #####
      Notifications are not indicative of the underlying content and might not always contain the entire data.  
      Notifier merely displays the contents as seen from the status bar.  

    + ##### Why don't the notification click/open actions work on some notifications? #####
      The notification actions are typically intended to be used only once.  
      Android therefore limits the number of times these actions can be fired presumably to avoid scenarios where a message is deleted twice, etc.  
      These actions also have a limited availability once their associated notifications are cleared off the staus bar, and can therefore fail occasionally when clicked from the 'cached notifications' group.  

  + #### How do I report a bug? ####
    https://github.com/niranjan-nagaraju/notification-center/issues/  
    If you are familiar with adb, you can collect debug logs using the below commands, and attach them to the issue.  
    ```
    $ adb logcat | grep bulletin_board
    OR
    $ adb logcat | grep com.vinithepooh.notifier
    ```

  + #### Can I contribute? ####
    Please feel free to fork/clone the repo and add your changes, or drop a pull-request with your changes.  
    
