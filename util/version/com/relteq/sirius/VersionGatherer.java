package com.relteq.sirius;

import java.io.File;
import java.util.Iterator;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Retrieves a hash of the latest Git commit
 */
public class VersionGatherer {

	public static void main(String[] args) {
		try{
			Git git = Git.open(new File("."));
			LogCommand cmd = git.log();
			cmd.setMaxCount(1);
			Iterable<RevCommit> res = cmd.call();
			Iterator<RevCommit> iter = res.iterator();
			if (!iter.hasNext()) throw new Exception("No commits");
			RevCommit commit = iter.next();
			System.out.println(commit.name());
			if (iter.hasNext()) System.err.println("Warning: more than 1 commit");
		} catch (Exception exc) {
			exc.printStackTrace();
			System.exit(-1);
		}
	}

}
