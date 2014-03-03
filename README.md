=======
linkshare: com.sakekasi.linkshare
=========

a webapp for easy link sharing and importing to browser bookmarks.

## Getting Started (DEV)

1. Start the application: `lein ring server` \*
2. Go to [localhost:10000](http://localhost:10000/) to see a basic overview of the REST API

## Configuration

To configure logging see config/logback.xml. By default, the app logs to stdout and logs/.
To learn more about configuring Logback, read its [documentation](http://logback.qos.ch/documentation.html).

TODO:

* fix bug in lookup function. long urls return not found
* fix bug in lookup function. lookup https urls, urls w/o titles
