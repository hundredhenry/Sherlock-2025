package uk.ac.warwick.dcs.sherlock.engine.storage.base;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.mysql.cj.protocol.a.MultiPacketReader;

import uk.ac.warwick.dcs.sherlock.api.registry.SherlockRegistry;
import uk.ac.warwick.dcs.sherlock.api.util.Side;
import uk.ac.warwick.dcs.sherlock.engine.SherlockEngine;
import uk.ac.warwick.dcs.sherlock.engine.storage.*;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.NGramDetector;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.JavaLexer;
import uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing.NGramPostProcessor;
import uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing.NGramRawResult;

import java.io.*;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BaseStorageFilesystemTest {

    SherlockEngine se = new SherlockEngine(Side.CLIENT);
    SherlockRegistry sr = new SherlockRegistry();
    BaseStorageFilesystem baseStorageFilesystem = new BaseStorageFilesystem();
    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void storeAndLoadFile() {

        byte[] inputFileBytes = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, \nsed do eiusmod tempor incididunt ut labore et dolore magna aliqua.".getBytes();
        byte[] returnedFileBytes = null;
        MultipartFile returnedFile = null;

        MultipartFile inputFile = new MockMultipartFile("testfile.txt", inputFileBytes);

        int line = 0;
        int contentLine = 0;
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(inputFileBytes)));

        try {
            String lineTxt;
            while ((lineTxt = reader.readLine()) != null) {
                line++;
                if (!lineTxt.isEmpty()) {
                    contentLine++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        EntityFile testEntityFile = new EntityFile(new EntityArchive(), FilenameUtils.getBaseName(inputFile.getName()), FilenameUtils.getExtension(inputFile.getName()), new Timestamp(System.currentTimeMillis()), inputFileBytes.length, line, contentLine);
        baseStorageFilesystem.storeFile(testEntityFile, inputFileBytes);
        InputStream returnedFileStream = baseStorageFilesystem.loadFile(testEntityFile);
        assertNotNull(returnedFileStream);
        try {
            returnedFile = new MockMultipartFile("testfile.txt", returnedFileStream);
            returnedFileBytes = returnedFile.getBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals(Arrays.toString(inputFileBytes), Arrays.toString(returnedFileBytes));
    }

    @Test
    void storeAndLoadFileAsString() {         
        byte[] inputFileBytes = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, \nsed do eiusmod tempor incididunt ut labore et dolore magna aliqua.".getBytes();
        MultipartFile inputFile = new MockMultipartFile("testfile.txt", inputFileBytes);

        int line = 0;
        int contentLine = 0;
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(inputFileBytes)));

        try {
            String lineTxt;
            while ((lineTxt = reader.readLine()) != null) {
                line++;
                if (!lineTxt.isEmpty()) {
                    contentLine++;
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        EntityFile testEntityFile = new EntityFile(new EntityArchive(), FilenameUtils.getBaseName(inputFile.getName()), FilenameUtils.getExtension(inputFile.getName()), new Timestamp(System.currentTimeMillis()), inputFileBytes.length, line, contentLine);
        boolean storeSuccess = baseStorageFilesystem.storeFile(testEntityFile, inputFileBytes);
        assertTrue(storeSuccess);
        String returnedFileString = baseStorageFilesystem.loadFileAsString(testEntityFile);
        assertNotNull(returnedFileString);
        assertEquals(new String(inputFileBytes), returnedFileString);
    }

    @Test
    void storeAndLoadTaskRawResults() {
        SherlockRegistry.registerLanguage("Java", JavaLexer.class);
        SherlockRegistry.registerDetector(NGramDetector.class);
        SherlockRegistry.registerPostProcessor(NGramPostProcessor.class, NGramRawResult.class);
        EntityJob entityJob = new EntityJob();
        EntityTask entityTask = new EntityTask(entityJob, NGramDetector.class);
        assertTrue(baseStorageFilesystem.storeTaskRawResults(entityTask));
    }

    @Disabled("Need to mess around with DB and filestorage a bit more to understand this method")
    @Test
    void validateFileStore() {
        EmbeddedDatabase database = new EmbeddedDatabase();
        //Do a scan of all files in database in background, check they exist and there are no extra files
        List orphans = baseStorageFilesystem.validateFileStore(database.runQuery("SELECT f from File f", EntityFile.class), database.runQuery("SELECT t from Task t", EntityTask.class));
        System.out.println(orphans.size());
        if (orphans != null && orphans.size() > 0) {
            database.removeObject(orphans);
        }

    }
}