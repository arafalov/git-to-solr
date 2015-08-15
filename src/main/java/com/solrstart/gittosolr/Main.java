package com.solrstart.gittosolr;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by arafalov on 6/14/14.
 */
public class Main {

    private static final String DEFAULT_SOLR_SERVER = "http://localhost:8983/solr/git";

    public static void main(String[] args) throws SolrServerException, IOException {
        if (args.length < 1) {
            System.err.println("Usage: java com.solrstart.gittosolr.Main repoDir [solrServerURL]");
            return;
        }
        String repoDirPath = args[0];
        String solrServer = (args.length == 2)? args[1]: DEFAULT_SOLR_SERVER;

        File repoDir = new File(repoDirPath);
        String repoName = repoDir.getName(); //just last component
        File gitDir = new File(repoDir, ".git");
        if (!gitDir.exists()) {
            System.err.println("Git directory is not found at: " + gitDir.getAbsolutePath());
            return;
        }

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repo = builder.setGitDir(gitDir).build();

        System.out.printf("Indexing %s\n", repo.getFullBranch());
        Ref head = repo.getRef(repo.getFullBranch()); //index starting from current branch
        RevWalk walk = new RevWalk(repo);

        RevCommit commit = walk.parseCommit(head.getObjectId());
        System.out.println("Start-Commit: " + commit);

        System.out.println("Walking all commits starting at HEAD");
        walk.markStart(commit);

        DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE); //we don't print out
        diffFormatter.setRepository(repo);
        diffFormatter.setContext(0); //we only want to index the changes
        diffFormatter.setDetectRenames(true);

        List<SolrInputDocument> solrDocumentList = new LinkedList<SolrInputDocument>();

        int count = 0;
        for (RevCommit rev : walk) {
            count++;
//            if (count == 100) { break; } //DEBUG BREAK

            System.out.println("\n\nCommit: " + rev);
            SolrInputDocument solrDoc = new SolrInputDocument();
            solrDocumentList.add(solrDoc); //before we forget
            solrDoc.addField("type", "commit");
            solrDoc.addField("id", rev.getName());
            solrDoc.addField("message", rev.getFullMessage());
            PersonIdent committerIdent = rev.getCommitterIdent();
            solrDoc.addField("committer", committerIdent.getName());
            solrDoc.addField("commitTime", committerIdent.getWhen());
            solrDoc.addField("committerEmail", committerIdent.getEmailAddress());
            //Ignore author vs. committer


            if(rev.getParentCount() == 0){
                System.out.println("NO PARENTS");
                //TODO: Add all files introduced in this first commit

                TreeWalk firstTreeWalk = new TreeWalk(repo);
                firstTreeWalk.addTree(rev.getTree());
                firstTreeWalk.setRecursive(true);

                int idx = 0;
                while (firstTreeWalk.next())
                {
                    SolrInputDocument diffSolrDoc = new SolrInputDocument();
                    diffSolrDoc.addField("id", rev.getName() + "-" + idx);
                    diffSolrDoc.addField("type", "diff");
                    diffSolrDoc.addField("diffType", DiffEntry.ChangeType.ADD); //first time, what else?
                    diffSolrDoc.addField("fileId",firstTreeWalk.getObjectId(0).name());
                    diffSolrDoc.addField("filePath", firstTreeWalk.getPathString());
                    System.out.println(diffSolrDoc);
                    solrDoc.addChildDocument(diffSolrDoc);
                    idx++;
                }

            } else {
                if (rev.getParentCount()>1) {
                    System.out.println("  Multiple parents: " + rev.getParentCount());
                }
                List<String> parentIds = new LinkedList<String>();
                for (RevCommit revParent : rev.getParents()) {
                    System.out.println("  parent: " + revParent);
                    parentIds.add(revParent.getName());
                }
                solrDoc.addField("parents", parentIds);

                List<DiffEntry> diffs = diffFormatter.scan(rev.getParent(0), rev); //first parent = current branch
                for (int diffIdx = 0; diffIdx < diffs.size(); diffIdx++) {
                    DiffEntry diffEntry = diffs.get(diffIdx);
                    SolrInputDocument diffSolrDoc = new SolrInputDocument();
                    diffSolrDoc.addField("id", rev.getName() + "-" + diffIdx); //anything else seems to cause dups
                    diffSolrDoc.addField("type", "diff");
                    DiffEntry.ChangeType diffType = diffEntry.getChangeType();
                    diffSolrDoc.addField("diffType", diffType);

                    switch (diffType) {
                        case ADD:
                        case COPY:
                        case RENAME:
                        case MODIFY:
                            diffSolrDoc.addField("fileId",diffEntry.getNewId().name());
                            diffSolrDoc.addField("filePath", diffEntry.getNewPath());
                    }
                    switch (diffType) {
                        case COPY:
                        case RENAME:
                        case DELETE:
                            diffSolrDoc.addField("fileOldPath", diffEntry.getOldPath());
                            //no break, we want to fall through to the next part

                        case MODIFY:
                            diffSolrDoc.addField("fileOldId", diffEntry.getOldId().name());
                    }
                    System.out.println(diffSolrDoc);
                    solrDoc.addChildDocument(diffSolrDoc);

                }
            }

            System.out.println(solrDoc);
        }
        System.out.println(count);

        walk.close();
        repo.close();


        System.out.println("Parsing!");
        HttpSolrClient solr = new HttpSolrClient(solrServer);
        solr.deleteByQuery("*:*");
        solr.add(solrDocumentList);
        solr.commit();
        SolrQuery query = new SolrQuery("*:*");
        QueryResponse response = solr.query(query);
        SolrDocumentList results = response.getResults();
        System.out.printf("Retrieved %d of %d\n", results.size(), results.getNumFound());


    }

}
