package no.kommune.bergen.tardis;

import org.eclipse.jgit.api.GarbageCollectCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;

@Component
class SnapshotStore {

    private Git git;

    public String log() {
        StringBuilder log = new StringBuilder();
        ObjectReader reader = null;
        try {
            reader = git.getRepository().newObjectReader();
            for (RevCommit commit : git.log().call()) {
                log.append(commit.getName()).append(" ");
                log.append(formatGitDate(commit.getCommitTime()));
                log.append(" ");
                log.append(commit.getShortMessage());
                log.append("\n");

                appendFiles(commit, log);

                log.append("\n\n");
            }
            return log.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (null != reader) reader.release();
        }
    }

    private void appendFiles(RevCommit commit, StringBuilder log) throws IOException {
        if (commit.getParentCount() == 0) return;
        RevCommit parent = commit.getParent(0);
        DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
        df.setRepository(git.getRepository());
        df.setDiffComparator(RawTextComparator.DEFAULT);
        int i = 0;
        for (DiffEntry diffEntry : df.scan(parent.getTree(), commit.getTree())) {
            log.append(diffEntry.getNewPath());
            log.append(", ");
            i++;
        }
        if (i > 0) log.setLength(log.length() - 2);
    }

    private String formatGitDate(int date) {
        DateFormat df = new UtcDateFormat();
        return df.format(new Date(date * 1000L));
    }


    @Autowired
    public void setConfiguration(TardisConfiguration configuration) {
        setWorkingDirectory(configuration.getWorkingDirectory());
    }

    void setWorkingDirectory(String workingDirectory) {
        try {
            File repository = new File(workingDirectory);

            if (!repository.exists()) repository.mkdirs();

            try {
                git = Git.open(repository);
            } catch (RepositoryNotFoundException e) {
                git = Git.init().setDirectory(repository).call();
                RevCommit initialCommit = git.commit().setMessage("initial commit").call();
                git.tag().setName("initial").setObjectId(initialCommit).call();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addSnapshot(String filename) {
        try {
            git.add().addFilepattern(filename).call();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void commit(String message) {
        try {
            Status status = git.status().call();
            if (status.isClean()) return;
            git.commit().setMessage(message).call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream getLatestSnapshot(String filename) {
        return getSnapshot(filename, new Date());
    }

    public InputStream getSnapshot(String filename, Date at) {
        ObjectReader reader = null;
        try {
            reader = git.getRepository().newObjectReader();
            RevCommit found = getCommitBefore(at, reader);
            if (found == null) return null;
            RevTree tree = found.getTree();
            TreeWalk treewalk = TreeWalk.forPath(reader, filename, tree);
            if (treewalk == null) return null;
            return reader.open(treewalk.getObjectId(0)).openStream();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) reader.release();
        }
    }

    byte[] getDiff(String filename, String fromRevstr, String toRevstr) {
        return getDiffInternal(filename, fromRevstr, toRevstr);
    }

    byte[] getDiff(String filename, Date from, Date to) {
        return getDiffInternal(filename, from, to);
    }

    private byte[] getDiffInternal(String filename, Object fromRevision, Object toRevision) {
        ObjectReader reader = null;
        try {
            reader = git.getRepository().newObjectReader();
            ObjectId fromCommit = resolveObjectId(fromRevision, reader);
            ObjectId toCommit = resolveObjectId(toRevision, reader);

            try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                git.diff()
                        .setOutputStream(out)
                        .setPathFilter(PathFilter.create(filename))
                        .setOldTree(createTreeIterator(fromCommit, reader))
                        .setNewTree(createTreeIterator(toCommit, reader))
                        .call();

                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {

            if (null != reader) reader.release();
        }
    }

    private AbstractTreeIterator createTreeIterator(ObjectId id, ObjectReader reader) throws Exception {
        CanonicalTreeParser parser = new CanonicalTreeParser();
        parser.reset(reader, new RevWalk(git.getRepository()).parseTree(id));
        return parser;
    }

    private RevCommit getCommitBefore(Date at, ObjectReader reader) throws Exception {
        long atSeconds = at.getTime() / 1000L;
        RevWalk walk = new RevWalk(reader);

        try {
            walk.markStart(walk.parseCommit(resolveObjectId("HEAD", reader)));

            for (RevCommit commit : walk) {
                if (commit.getCommitTime() <= atSeconds) return commit;
            }
        } finally {
            walk.dispose();
        }
        return getInitialCommit(reader);
    }

    private RevCommit getInitialCommit(ObjectReader reader) throws Exception {
        RevWalk revWalk = new RevWalk(git.getRepository());
        RevCommit initial = revWalk.parseCommit(resolveObjectId("initial", reader));
        revWalk.dispose();
        return initial;
    }

    private ObjectId resolveObjectId(Object revision, ObjectReader reader) throws Exception {
        if (revision instanceof String) return git.getRepository().resolve((String) revision);
        if (revision instanceof Date) return getCommitBefore((Date) revision, reader).getId();
        throw new IllegalArgumentException("Revision must be either String or Date");
    }

    public Properties garbageCollect() {
        GarbageCollectCommand gc = git.gc();
        try {
            return gc.call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }
}
