package org.jenkinsci.test.acceptance.plugins.git;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.test.acceptance.docker.fixtures.GitContainer;
import org.zeroturnaround.zip.ZipUtil;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import static java.lang.ProcessBuilder.Redirect.INHERIT;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.util.Collections.singleton;
import static org.jenkinsci.test.acceptance.docker.fixtures.GitContainer.*;

/**
 * Manipulates git repository locally.
 * <p/>
 *
 * @author Kohsuke Kawaguchi
 */
public class GitRepo implements Closeable {
    public final File dir;

    /**
     * Path to the script that acts like SSH.
     */
    private File ssh;

    /**
     * Private key file that contains /ssh_keys/unsafe.
     */
    private File privateKey;

    public GitRepo() throws IOException, InterruptedException {
        dir = initDir();
        git("init");
        setIdentity(dir);
    }

    /**
     * Creates a new repo by cloning the given URL.
     */
    public GitRepo(String url) throws IOException, InterruptedException {
        dir = initDir();
        git("clone", url, ".");
        setIdentity(dir);
    }

    /**
     * Configures an identity for the repo.
     */
    private void setIdentity(File dir) throws IOException, InterruptedException {
        gitDir(dir, "config", "user.name", "Jenkins-ATH");
        gitDir(dir, "config", "user.email", "jenkins-ath@example.org");
    }

    private File initDir() throws IOException {
        // FIXME: perhaps this logic that makes it use a separate key should be moved elsewhere?
        privateKey = File.createTempFile("ssh", "key");
        FileUtils.copyURLToFile(GitContainer.class.getResource("GitContainer/unsafe"), privateKey);
        Files.setPosixFilePermissions(privateKey.toPath(), singleton(OWNER_READ));

        ssh = File.createTempFile("jenkins", "ssh");
        FileUtils.writeStringToFile(ssh,
                "#!/bin/sh\n" +
                        "exec ssh -o StrictHostKeyChecking=no -i " + privateKey.getAbsolutePath() + " \"$@\"");
        Files.setPosixFilePermissions(ssh.toPath(), new HashSet<>(Arrays.asList(OWNER_READ, OWNER_EXECUTE)));

        return createTempDir("git");
    }

    public void git(Object... args) throws IOException, InterruptedException {
        gitDir(this.dir, args);
    }

    /**
     * Execute git command in specified directory.
     */
    public void gitDir(File dir, Object... args) throws IOException, InterruptedException {
        List<String> cmds = new ArrayList<>();
        cmds.add("git");
        for (Object a : args) {
            if (a != null)
                cmds.add(a.toString());
        }
        ProcessBuilder pb = new ProcessBuilder(cmds);
        pb.environment().put("GIT_SSH", ssh.getAbsolutePath());

        int r = pb.directory(dir)
                .redirectInput(INHERIT)
                .redirectError(INHERIT)
                .redirectOutput(INHERIT).start().waitFor();
        if (r != 0)
            throw new Error(cmds + " failed");
    }

    /**
     * Creates a new commit, with a file named "foo".
     */
    public void add_foo_and_commit(String msg) throws IOException, InterruptedException {
        try (FileWriter o = new FileWriter(new File(dir, "foo"), true)) {
            o.write("more");
        }
        git("add", "foo");
        git("commit", "-m", msg);
    }

    /**
     * Creates a new git repo, with a file named "filename", and commits.
     */
    public void add_file_and_commit(String msg, String filename) throws IOException, InterruptedException {
        try (FileWriter o = new FileWriter(new File(dir, filename), true)) {
            o.write("text");
        }
        git("add", filename);
        git("commit", "-m", msg);
    }

    /**
     *
     * appends a String to a text file
     */
    public void append_string_to_file(String filename, String append) throws IOException {

        try {
            Files.write(Paths.get(filename), append.getBytes(), StandardOpenOption.APPEND);
        }
        catch (IOException x) {
            System.err.format("Jenkins Acceptance Test Harness: IOException: %s%n", x);
        }
    }

    @Override
    public void close() throws IOException {
        FileUtils.deleteDirectory(dir);
        ssh.delete();
        privateKey.delete();
    }

    /**
     * Add a submodule to the main repository.
     * @param submoduleName Name of the submodule
     */
    public GitRepo addSubmodule(String submoduleName) throws IOException, InterruptedException {
        File submoduleDir = new File(createTempDir(submoduleName).getAbsolutePath() + "/" + submoduleName);
        submoduleDir.delete();
        submoduleDir.mkdir();

        gitDir(submoduleDir, "init");
        setIdentity(submoduleDir);
        try (FileWriter o = new FileWriter(new File(submoduleDir, "foo"), true)) {
            o.write("more");
        }

        gitDir(submoduleDir, "add", "foo");
        gitDir(submoduleDir, "commit", "-m", "Initial commit");

        git("submodule", "add", submoduleDir.getAbsolutePath());
        git("commit", "-am", "Added submodule");

        return this;
    }

    public File createTempDir(String name) throws IOException {
        File tmp = File.createTempFile("jenkins", name);
        tmp.delete();
        tmp.mkdir();
        return tmp;
    }

    /**
     * Zip bare repository, copy to Docker container using sftp, then unzip.
     * The repo is now accessible over "ssh://git@ip:port/home/git/gitRepo.git"
     *
     * @param host IP of Docker container
     * @param port SSH port of Docker container
     */
    public void transferToDockerContainer(String host, int port) throws IOException, InterruptedException, JSchException, SftpException {

        String zippedFilename = "repo.zip";
        File zipppedRepo = new File(dir.getPath() + "/" + zippedFilename);
        ZipUtil.pack(new File(dir.getPath()), zipppedRepo);

        Properties props = new Properties();
        props.put("StrictHostKeyChecking", "no");

        JSch jSch = new JSch();
        jSch.addIdentity(privateKey.getAbsolutePath());

        Session session = jSch.getSession("git", host, port);
        session.setConfig(props);
        session.connect();

        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
        channel.cd("/home/git");
        channel.put(new FileInputStream(zipppedRepo), zippedFilename);

        ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
        InputStream in = channelExec.getInputStream();
        channelExec.setCommand("unzip " + zippedFilename + " -d " + REPO_NAME);
        channelExec.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        int index = 0;
        while ((line = reader.readLine()) != null) {
            System.out.println(++index + " : " + line);
        }

        channelExec.disconnect();
        channel.disconnect();
        session.disconnect();
    }
}
