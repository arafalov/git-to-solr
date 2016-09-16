git-to-solr
===========

Index git history into a Solr repository. Represents commit's content (file changed, etc) as nested documents. Tested against Solr 4.9 .

Inspired by [Gary Sieling's solr-git](https://github.com/garysieling/solr-git) and [JGit-Cookbook](https://github.com/centic9/jgit-cookbook).

Start Solr server by running **java -Dsolr.solr.home=\<project-root\>/solr -jar start.jar** from Solr distribution's example directory.

bin/solr create_core -c git -d ~/Projects/git-to-solr/solr/collection1/

To build the jar, you need to use [Apache Maven](http://maven.apache.org/) and run **mvn clean compile assembly:single** at the top of the directory (where pom.xml is)

Query examples:
http://localhost:8983/solr/git/select?q={!parent+which%3D%22type%3Acommit%22}diffType%3AADD&wt=json&indent=true&fl=*,[child%20parentFilter=type:commit]
(decoded) http://localhost:8983/solr/git/select?q={!parent which="type:commit"}diffType:ADD&wt=json&indent=true&fl=*,[child parentFilter=type:commit]

Show commit (parents) that have ADD operations in them, include all nested children (whether they are ADD or MODIFY or whatever).


?Possible
- Average number of different operations (ADD/MODIFY) per commit