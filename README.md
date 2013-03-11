# Stash Plugin to Notify Jenkins
A basic post-receive hook that notifies Jenkins when changes are pushed to Stash.  This plugin requires the Git Plugin to be installed on your Jenkins server (https://wiki.jenkins-ci.org/display/JENKINS/Git+Plugin)
To specifiy a default Jenkins URL, startup Stash with JAVA_OPTS="-Djenkins.url=http://foo.com"