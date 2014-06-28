Solr installation is already running inside a web server (Jetty) by default.
Might as well use it for our own UI, as that allows to call to the Solr backend without any javascript restrictions.

Copy the *git-solr-jetty-context.xml* file to *\<solr-installation\>/example/contexts* directory.
Nothing needs to be changed, as the reference is relative to our Solr collection home, which is also in this same project.