=======
linkshare: com.sakekasi.linkshare
=========

a webapp for easy link sharing and importing to browser bookmarks.

## Getting Started (DEV)

1. Start the application: `lein run-dev` \*
2. Go to [localhost:8080](http://localhost:8080/) to see: the rest guide
3. Read your app's source code at src/com/sakekasi/linkshare/service.clj. Explore the docs of functions
   that define routes and responses.
4. Run your app's tests with `lein test`. Read the tests at test/com/sakekasi/linkshare/service_test.clj.
5. Learn more! See the [Links section below](#links).

\* `lein run-dev` automatically detects code changes. Alternatively, you can run in production mode
with `lein run`.

## Configuration

To configure logging see config/logback.xml. By default, the app logs to stdout and logs/.
To learn more about configuring Logback, read its [documentation](http://logback.qos.ch/documentation.html).

## TODO
* write test code for basic API sanity
* write log code on server side(io.pedestal.service.log)

