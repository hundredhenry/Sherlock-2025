package uk.ac.warwick.dcs.sherlock.module.model.base.detection;

import org.eclipse.persistence.annotations.TenantTableDiscriminator;
import org.hibernate.annotations.TimeZoneStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import uk.ac.warwick.dcs.sherlock.api.model.detection.IDetector;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.PreProcessingStrategy;
import uk.ac.warwick.dcs.sherlock.api.util.PairedTuple;
import uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing.NGramRawResult;
import uk.ac.warwick.dcs.sherlock.api.util.ITuple;
import uk.ac.warwick.dcs.sherlock.api.util.Tuple;
import uk.ac.warwick.dcs.sherlock.engine.storage.EntityArchive;
import uk.ac.warwick.dcs.sherlock.engine.storage.EntityFile;
import uk.ac.warwick.dcs.sherlock.engine.storage.EntityWorkspace;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test harness to test whether skeleton code is removed properly within RawResult classes
 */
class SkeletonCodeTest {

    //The two test files that we will be matching
    private EntityFile testFile1;
    private EntityFile testFile2;
    //The Raw Result class that stores the matches for the above test files. This could be any RawResult
    // class, since they all use the same methods (at least for NGram, AST and VarName) inhereted from AbstractModelRawResult
    private NGramRawResult<NGramMatch> rawLegitimateResult;

    /**
     * Runs before each test, essentially just creates the archives for the files involved in the
     *  tests. Also creates all of the files being used in the matches.
     */
    @BeforeEach
    void setUp() {
        //Creates the archive to store all of the legitimate tests - usually would need two archives
        // but for testing this does not matter
        EntityArchive testArchive = new EntityArchive("TestArchive");
        //Creates both of the test files and stores them within the archive
        testFile1 = new EntityFile(
            testArchive, "TestFile.java", "java", new Timestamp(1), 50, 50, 50
        );
        testFile2 = new EntityFile(
            testArchive, "TestFile2.java", "java", new Timestamp(1), 50, 50, 50
        );

        //then create the RawResult class between the two legitimate files
        rawLegitimateResult = new NGramRawResult<>(testFile1, testFile2);

        //and add some matches to it
        NGramMatch match;
        //adding a match which says lines 10-15 in file 1 match to lines 13-20 in file 2 (inc.)
        match = new NGramMatch(10, 15, 13, 20, 0.5f, testFile1, testFile2);
        rawLegitimateResult.put(match, 10,15,13,20);
        //adding a match which says lines 16-20 in file 1 match to lines 21-25 in file 2 (inc.)
        match = new NGramMatch(16, 20, 21,25, 0.5f, testFile1, testFile2);
        rawLegitimateResult.put(match, 16,20,21,25);
    }

    @AfterEach
    void tearDown() {
    }


    /**
     * Tests what happens when trying to remove skeleton code which has ranges outside of either
     *  of the matches stored within the RawResult class
     */
    @Test
    void lineRemovalOutsideOfRange() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25
        //removing 1-1 & 1-1 shouldnt affect anything (other than reversing the results)

        //remove the match 1-1 & 1-1
        rawLegitimateResult.removeLine(new PairedTuple<>(1,1,1,1));
        //fetch the new locationss
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        //check only two locations still
        assertEquals(2, locations.size());
        //then get them in reverse order and make sure that the values are correct
        PairedTuple<Integer, Integer, Integer, Integer> location;
        //checking second range is 10-15 & 13-20
        location = locations.get(1);
        assertEquals(10, location.getPoint1().getKey());
        assertEquals(15, location.getPoint1().getValue());
        assertEquals(13, location.getPoint2().getKey());
        assertEquals(20, location.getPoint2().getValue());
        //checking first range is 16-20 & 21-25
        location = locations.get(0);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(20, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(25, location.getPoint2().getValue());

        //then double check the stores objects are also the same
        List<NGramMatch> matches = rawLegitimateResult.getObjects();
        //check only two matches
        assertEquals(2, matches.size());
        ArrayList<ITuple<Integer, Integer>> matchLines;
        //check second range is 10-15 & 13-20
        matchLines = matches.get(1).lines;
        assertEquals(10, matchLines.get(0).getKey());
        assertEquals(15, matchLines.get(0).getValue());
        assertEquals(13, matchLines.get(1).getKey());
        assertEquals(20, matchLines.get(1).getValue());
        //check first range is 16-20 & 21-25
        matchLines = matches.get(0).lines;
        assertEquals(16, matchLines.get(0).getKey());
        assertEquals(20, matchLines.get(0).getValue());
        assertEquals(21, matchLines.get(1).getKey());
        assertEquals(25, matchLines.get(1).getValue());
    }

    /**
     * Checks what happens when removing skeleton code that encompasses both ranges
     */
    @Test
    void lineRemovalFullyEncompassesAllRanges() {
        //removing 1-30 & 1-30 should remove both matches entirely
        rawLegitimateResult.removeLine(new PairedTuple<>(1,30,1,30));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        //check no matches in locations or objects
        assertEquals(0, locations.size());
        List<NGramMatch> matches = rawLegitimateResult.getObjects();
        assertEquals(0, matches.size());
    }

    /**
     * Checking what happens when removing skeleton code that full encompasses one match
     */
    @Test
    void lineRemovalFullyEncompassesOneRange() {
        //removing explicitly 10-15 & 13-20 should only remove that range, leaving only 16-20 & 21-25
        rawLegitimateResult.removeLine(new PairedTuple<>(10,15,13,20));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        //check only one range
        assertEquals(1, locations.size());
        //and check it is 16-20 & 21-25
        PairedTuple<Integer, Integer, Integer, Integer> location = locations.get(0);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(20, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(25, location.getPoint2().getValue());

        //check the same thing is represented in the objects
        List<NGramMatch> matches = rawLegitimateResult.getObjects();
        assertEquals(1, matches.size());
        ArrayList<ITuple<Integer, Integer>> matchLines = matches.get(0).lines;
        assertEquals(16, matchLines.get(0).getKey());
        assertEquals(20, matchLines.get(0).getValue());
        assertEquals(21, matchLines.get(1).getKey());
        assertEquals(25, matchLines.get(1).getValue());
    }

    /**
     * Tests that removing a line that encompasses one range of a match entirely, works as intended
     */
    @Test
    void lineRemovalPartiallyEncompassesOneRange() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25 
        //removing 1-15 & 1-1, should fully remove the first range
        rawLegitimateResult.removeLine(new PairedTuple<>(10,15,1,1));
        //check that there are only one stored match within locations
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        assertEquals(1, locations.size());
        //and that its the second range of 16-20 & 21-25
        PairedTuple<Integer, Integer, Integer, Integer> location = locations.get(0);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(20, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(25, location.getPoint2().getValue());

        //and check the same thing for any stored objects
        List<NGramMatch> matches = rawLegitimateResult.getObjects();
        assertEquals(1, matches.size());
        ArrayList<ITuple<Integer, Integer>> matchLines = matches.get(0).lines;
        assertEquals(16, matchLines.get(0).getKey());
        assertEquals(20, matchLines.get(0).getValue());
        assertEquals(21, matchLines.get(1).getKey());
        assertEquals(25, matchLines.get(1).getValue());
    }

    /**
     * Checks that removing a match which encompasses only one file's range, but for two differnt matches, removes both
     *  matches from the RawResult
     */
    @Test 
    void lineRemovalPartiallyEncompassesTwoRanges() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25 
        //removing 10-15 & 21-25, should fully remove both ranges
        rawLegitimateResult.removeLine(new PairedTuple<>(10,15,21,25));
        //check there are no matches still stored
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        assertEquals(0, locations.size());

        //and same for objects
        List<NGramMatch> matches = rawLegitimateResult.getObjects();
        assertEquals(0, matches.size());
    }

    /**
     * Check that removing a match which exists across a boundary of one range of one match, just trims that
     *  range to be smaller, rather than removing it. Also checks that the other match is unaffected
     */
    @Test 
    void lineRemovalTrimsOneLeftBoundary() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25
        //removing 8-12 & 1-1 should affect first range, setting it to 13-15 & 13-20, but not affecting second range
        rawLegitimateResult.removeLine(new PairedTuple<>(8,12,1,1));
        //get the stored locations
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        //check that there are two ranges still
        assertEquals(2, locations.size());
        //check that the second one is the trimmed range
        PairedTuple<Integer, Integer, Integer, Integer> location;
        location = locations.get(1);
        assertEquals(13, location.getPoint1().getKey());
        assertEquals(15, location.getPoint1().getValue());
        assertEquals(13, location.getPoint2().getKey());
        assertEquals(20, location.getPoint2().getValue());
        //and that the first one is the unchanged range
        location = locations.get(0);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(20, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(25, location.getPoint2().getValue());

        //then check the same thing for the objects stored
        List<NGramMatch> matches = rawLegitimateResult.getObjects();
        assertEquals(2, matches.size());
        ArrayList<ITuple<Integer, Integer>> matchLines;
        matchLines = matches.get(1).lines;
        assertEquals(13, matchLines.get(0).getKey());
        assertEquals(15, matchLines.get(0).getValue());
        assertEquals(13, matchLines.get(1).getKey());
        assertEquals(20, matchLines.get(1).getValue());
        matchLines = matches.get(0).lines;
        assertEquals(16, matchLines.get(0).getKey());
        assertEquals(20, matchLines.get(0).getValue());
        assertEquals(21, matchLines.get(1).getKey());
        assertEquals(25, matchLines.get(1).getValue());
    }

    /**
     * Testing that removing a range that overlaps the left boundary of both ranges within a single match
     *  trims both of them
     */
    void lineRemovalTrimsBothLeftBoundaries() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25
        //removing 8-12 & 11-15 should affect both ranges, setting them to 13-15 & 16-20, but not affecting second range
        rawLegitimateResult.removeLine(new PairedTuple<>(8,12,11,15));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        //check that there are still only two matches
        assertEquals(2, locations.size());
        PairedTuple<Integer, Integer, Integer, Integer> location;
        //get the second one and check both ranges have been trimmed appropriately
        location = locations.get(1);
        assertEquals(13, location.getPoint1().getKey());
        assertEquals(15, location.getPoint1().getValue());
        assertEquals(16, location.getPoint2().getKey());
        assertEquals(20, location.getPoint2().getValue());
        //get the first and make sure that they are unchanged
        location = locations.get(0);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(20, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(25, location.getPoint2().getValue());

        //and check the same for objects
        List<NGramMatch> matches = rawLegitimateResult.getObjects();
        assertEquals(2, matches.size());
        ArrayList<ITuple<Integer, Integer>> matchLines;
        matchLines = matches.get(1).lines;
        assertEquals(13, matchLines.get(0).getKey());
        assertEquals(15, matchLines.get(0).getValue());
        assertEquals(16, matchLines.get(1).getKey());
        assertEquals(20, matchLines.get(1).getValue());
        matchLines = matches.get(0).lines;
        assertEquals(16, matchLines.get(0).getKey());
        assertEquals(20, matchLines.get(0).getValue());
        assertEquals(21, matchLines.get(1).getKey());
        assertEquals(25, matchLines.get(1).getValue());
    }

    /**
     * Check that removing a match which exists across a right boundary of one range of one match, just trims that
     *  range to be smaller, rather than removing it. Also checks that the other match is unaffected
     */
    @Test 
    void lineRemovalTrimsOneRightBoundary() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25
        //removing 18-21 & 1-1 should affect second range, setting it to 16-17 & 21-25, but not affecting first range
        rawLegitimateResult.removeLine(new PairedTuple<>(18,21,1,1));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        //check that there are still two matches
        assertEquals(2, locations.size());
        PairedTuple<Integer, Integer, Integer, Integer> location;
        //check that the second one is unchanged
        location = locations.get(1);
        assertEquals(10, location.getPoint1().getKey());
        assertEquals(15, location.getPoint1().getValue());
        assertEquals(13, location.getPoint2().getKey());
        assertEquals(20, location.getPoint2().getValue());
        //and the first one has had the right boundary of the first range trimmed
        location = locations.get(0);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(17, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(25, location.getPoint2().getValue());

        //and check the same for the objects stored
        List<NGramMatch> matches = rawLegitimateResult.getObjects();
        assertEquals(2, matches.size());
        ArrayList<ITuple<Integer, Integer>> matchLines;
        matchLines = matches.get(1).lines;
        assertEquals(10, matchLines.get(0).getKey());
        assertEquals(15, matchLines.get(0).getValue());
        assertEquals(13, matchLines.get(1).getKey());
        assertEquals(20, matchLines.get(1).getValue());
        matchLines = matches.get(0).lines;
        assertEquals(16, matchLines.get(0).getKey());
        assertEquals(17, matchLines.get(0).getValue());
        assertEquals(21, matchLines.get(1).getKey());
        assertEquals(25, matchLines.get(1).getValue());
    }

    /**
     * Testing that removing a range that overlaps the right boundary of both ranges within a single match
     *  trims both of them
     */
    @Test 
    void lineRemovalTrimsBothRightBoundaries() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25
        //removing 18-21 & 23-26 should affect both ranges, setting them to 16-17 & 21-22, but not affecting first range
        rawLegitimateResult.removeLine(new PairedTuple<>(18,21,23,26));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        //check that there are still two matches
        assertEquals(2, locations.size());
        PairedTuple<Integer, Integer, Integer, Integer> location;
        //check that the second match is unchanged
        location = locations.get(1);
        assertEquals(10, location.getPoint1().getKey());
        assertEquals(15, location.getPoint1().getValue());
        assertEquals(13, location.getPoint2().getKey());
        assertEquals(20, location.getPoint2().getValue());
        //check that both ranges in the first match have been trimmed appropriately
        location = locations.get(0);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(17, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(22, location.getPoint2().getValue());

        //and check the same thing for the objects stored in RawResult class
        List<NGramMatch> matches = rawLegitimateResult.getObjects();
        assertEquals(2, matches.size());
        ArrayList<ITuple<Integer, Integer>> matchLines;
        matchLines = matches.get(1).lines;
        assertEquals(10, matchLines.get(0).getKey());
        assertEquals(15, matchLines.get(0).getValue());
        assertEquals(13, matchLines.get(1).getKey());
        assertEquals(20, matchLines.get(1).getValue());
        matchLines = matches.get(0).lines;
        assertEquals(16, matchLines.get(0).getKey());
        assertEquals(17, matchLines.get(0).getValue());
        assertEquals(21, matchLines.get(1).getKey());
        assertEquals(22, matchLines.get(1).getValue());
    }

    /**
     * Checks that when removing a range that exists within a range of one files range in a match that
     *  internal skeleton code is defined correctly
     */
    @Test
    void lineRemovalWithinOneRangeOneFile() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25
        //removing 11-12 & 1-1 should affect first range, ie we should have:

        //internalFile1SkeletonCode: <10,15> = [<11,12>]
        //internalFile2SkeletonCode: Empty
        //locations/objects = <<16,20>,<21,25>>, <<10,15>,<13,20>>
        rawLegitimateResult.removeLine(new PairedTuple(11,12,1,1));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        //check that there are still two matches
        assertEquals(2, locations.size());
        PairedTuple<Integer, Integer, Integer, Integer> location;
        //and check that both the first and second are unchanged
        location = locations.get(1);
        assertEquals(10, location.getPoint1().getKey());
        assertEquals(15, location.getPoint1().getValue());
        assertEquals(13, location.getPoint2().getKey());
        assertEquals(20, location.getPoint2().getValue());
        location = locations.get(0);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(20, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(25, location.getPoint2().getValue());

        //and check that they are unchanged for the objects as well
        List<NGramMatch> matches = rawLegitimateResult.getObjects();
        assertEquals(2, matches.size());
        ArrayList<ITuple<Integer, Integer>> matchLines;
        matchLines = matches.get(1).lines;
        assertEquals(10, matchLines.get(0).getKey());
        assertEquals(15, matchLines.get(0).getValue());
        assertEquals(13, matchLines.get(1).getKey());
        assertEquals(20, matchLines.get(1).getValue());
        matchLines = matches.get(0).lines;
        assertEquals(16, matchLines.get(0).getKey());
        assertEquals(20, matchLines.get(0).getValue());
        assertEquals(21, matchLines.get(1).getKey());
        assertEquals(25, matchLines.get(1).getValue());

        //then check internal skeleton code
        HashMap<ITuple,HashSet<ITuple<Integer, Integer>>> iscs = rawLegitimateResult.getInternalSkeletonCode(1);
        //check that the first file contains internal skeleton code
        assertEquals(1, iscs.size());
        //Then check that <10,15> exists as a key within the hashmap
        ITuple<Integer, Integer> key = new Tuple<>(10,15);
        assertTrue(iscs.containsKey(key));
        //and get the list from it
        HashSet<ITuple<Integer, Integer>> isc = iscs.get(key);
        //check that there is only one internal skeleton code range for this
        assertEquals(1, isc.size());
        //and check it is the right value
        assertTrue(isc.contains(new Tuple<>(11,12)));

        //then check that the second file does not have any internal skeleton code stored.
        iscs = rawLegitimateResult.getInternalSkeletonCode(2);
        assertEquals(0, iscs.size());
    }

    /**
     * Checks that when removing a range, in which both ranges in a match fully encompass both associated
     *  ranges to be removed, that internal skeleton code is added for both files, and is correct.
     */
    @Test
    void lineRemovalWithinOneRangeTwoFiles() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25
        //removing 11-12 & 15-17 should mean that we have:

        //internalFile1SkeletonCode: <10,15> = [<11,12>]
        //internalFile2SkeletonCode: <13,20> = [<15,17>]
        //locations/objects = <<16,20>,<21,25>>, <<10,15>,<13,20>>
        rawLegitimateResult.removeLine(new PairedTuple(11,12,15,17));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        //check that both matches are unaffected
        assertEquals(2, locations.size());
        PairedTuple<Integer, Integer, Integer, Integer> location;
        location = locations.get(1);
        assertEquals(10, location.getPoint1().getKey());
        assertEquals(15, location.getPoint1().getValue());
        assertEquals(13, location.getPoint2().getKey());
        assertEquals(20, location.getPoint2().getValue());
        location = locations.get(0);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(20, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(25, location.getPoint2().getValue());

        //and check that the objects are both unaffected
        List<NGramMatch> matches = rawLegitimateResult.getObjects();
        assertEquals(2, matches.size());
        ArrayList<ITuple<Integer, Integer>> matchLines;
        matchLines = matches.get(1).lines;
        assertEquals(10, matchLines.get(0).getKey());
        assertEquals(15, matchLines.get(0).getValue());
        assertEquals(13, matchLines.get(1).getKey());
        assertEquals(20, matchLines.get(1).getValue());
        matchLines = matches.get(0).lines;
        assertEquals(16, matchLines.get(0).getKey());
        assertEquals(20, matchLines.get(0).getValue());
        assertEquals(21, matchLines.get(1).getKey());
        assertEquals(25, matchLines.get(1).getValue());


        //then check internal skeleton code
        HashMap<ITuple,HashSet<ITuple<Integer, Integer>>> iscs = rawLegitimateResult.getInternalSkeletonCode(1);
        //check that the first file contains one internal skeleton code map
        assertEquals(1, iscs.size());
        //check it has the correct key
        ITuple<Integer,Integer> key = new Tuple<>(10,15);
        assertTrue(iscs.containsKey(key));
        //and that the key has just one internal skeleton code value associated to it
        HashSet<ITuple<Integer, Integer>> isc = iscs.get(key);
        assertEquals(1, isc.size());
        //and check it is correct
        assertTrue(isc.contains(new Tuple<>(11,12)));

        //chcek the same for the second file
        iscs = rawLegitimateResult.getInternalSkeletonCode(2);
        assertEquals(1, iscs.size());
        //check that it contains the correct range as a key
        key=new Tuple<>(13,20);
        assertTrue(iscs.containsKey(key));
        //and check that there is one internal range within that range
        isc = iscs.get(key);
        assertEquals(1, isc.size());
        //and check the value is correct
        assertTrue(isc.contains(new Tuple<>(15,17)));
    }

    /**
     * Testing that when creating internal skeleton code, trimming the initial range should
     *  create a new entry in the ISC map, maintianing the stored internal skeleton code
     */
    @Test 
    void internalSCThenRemoveEdgeNoChange() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25
        //removing 11-12 & 1-1 should affect first range, ie we should have:

        //internalFile1SkeletonCode: <10,15> = [<11,12>]
        //internalFile2SkeletonCode: Empty
        //locations/objects = <<16,20>,<21,25>>, <<10,15>,<13,20>>
        rawLegitimateResult.removeLine(new PairedTuple(11,12,1,1));
        //this has already been tested.

        //But now, removing 14-15 and 1-1 should change the code as such:
        //internalFile1SkeletonCode: <10,13> = [<11,12>] | <10,15> = [<11,12>]
        //internalFile2SkeletonCode: Empty
        //locations/objects = <<10,13>,<13,20>>, <<16,20>,<21,25>>
        rawLegitimateResult.removeLine(new PairedTuple(14,15,1,1));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        //check that the location match ranges are correct for both entries
        assertEquals(2, locations.size());
        PairedTuple<Integer, Integer, Integer, Integer> location;
        location = locations.get(0);
        assertEquals(10, location.getPoint1().getKey());
        assertEquals(13, location.getPoint1().getValue());
        assertEquals(13, location.getPoint2().getKey());
        assertEquals(20, location.getPoint2().getValue());
        location = locations.get(1);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(20, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(25, location.getPoint2().getValue());

        //and check that the ranges are correct for the objects stored
        List<NGramMatch> matches = rawLegitimateResult.getObjects();
        assertEquals(2, matches.size());
        ArrayList<ITuple<Integer, Integer>> matchLines;
        matchLines = matches.get(0).lines;
        assertEquals(10, matchLines.get(0).getKey());
        assertEquals(13, matchLines.get(0).getValue());
        assertEquals(13, matchLines.get(1).getKey());
        assertEquals(20, matchLines.get(1).getValue());
        matchLines = matches.get(1).lines;
        assertEquals(16, matchLines.get(0).getKey());
        assertEquals(20, matchLines.get(0).getValue());
        assertEquals(21, matchLines.get(1).getKey());
        assertEquals(25, matchLines.get(1).getValue());

        //then check internal skeleton code
        HashMap<ITuple,HashSet<ITuple<Integer, Integer>>> iscs = rawLegitimateResult.getInternalSkeletonCode(1);
        //The first file should now have two entries
        assertEquals(2, iscs.size());
        //check the two ranges are in the hasmap
        assertTrue(iscs.containsKey(new Tuple<>(10,13)));
        assertTrue(iscs.containsKey(new Tuple<>(10,15)));
        //check that the first range has the correct internal skeleton code associated to it
        HashSet<ITuple<Integer, Integer>> isc = iscs.get(new Tuple<>(10,13));
        assertEquals(1, isc.size());
        assertTrue(isc.contains(new Tuple<>(11,12)));
        //then check the second range has the correct ISC associated to it
        isc = iscs.get(new Tuple<>(10,15));
        assertEquals(1, isc.size());
        assertTrue(isc.contains(new Tuple<>(11,12)));

        //and check that the second file has no ISC stored
        iscs = rawLegitimateResult.getInternalSkeletonCode(2);
        assertEquals(0, iscs.size());
    }

    /**
     * Testing that after adding internal skeleton code, if the range of a file's match shrinks, such
     *  that the internal skeleton code becomes external, then the internal skeleton code is also removed
     *   from the range.
     */
    @Test 
    void internalSCThenRemoveEdgeWithChange() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25
        //removing 11-12 & 1-1 should affect first range, ie we should have:
        //internalFile1SkeletonCode: <10,15> = [<11,12>]
        //internalFile2SkeletonCode: Empty
        //locations/objects = <<16,20>,<21,25>>, <<10,15>,<13,20>>

        rawLegitimateResult.removeLine(new PairedTuple(11,12,1,1));
        //this has already been tested.

        //But now, removing 10-10 and 1-1 should change the code as such:
        //internalFile1SkeletonCode: <10,15> = [<11,12>]
        //internalFile2SkeletonCode: Empty
        //locations/objects = <<13,15>,<13,20>>, <<16,20>,<21,25>>
        //ie, both 10-10 & 1-1 was removed and also 11-12 & 1-1.

        rawLegitimateResult.removeLine(new PairedTuple(10,10,1,1));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        //check that there are still two matches
        assertEquals(2, locations.size());
        PairedTuple<Integer, Integer, Integer, Integer> location;
        //check that the first one is now 13-15 & 13-20, as expected
        location = locations.get(0);
        assertEquals(13, location.getPoint1().getKey());
        assertEquals(15, location.getPoint1().getValue());
        assertEquals(13, location.getPoint2().getKey());
        assertEquals(20, location.getPoint2().getValue());
        //check that the second one is unchanged
        location = locations.get(1);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(20, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(25, location.getPoint2().getValue());

        //and check the objects stored are also correct
        List<NGramMatch> matches = rawLegitimateResult.getObjects();
        assertEquals(2, matches.size());
        ArrayList<ITuple<Integer, Integer>> matchLines;
        matchLines = matches.get(0).lines;
        assertEquals(13, matchLines.get(0).getKey());
        assertEquals(15, matchLines.get(0).getValue());
        assertEquals(13, matchLines.get(1).getKey());
        assertEquals(20, matchLines.get(1).getValue());
        matchLines = matches.get(1).lines;
        assertEquals(16, matchLines.get(0).getKey());
        assertEquals(20, matchLines.get(0).getValue());
        assertEquals(21, matchLines.get(1).getKey());
        assertEquals(25, matchLines.get(1).getValue());

        //then check internal skeleton code
        HashMap<ITuple,HashSet<ITuple<Integer, Integer>>> iscs = rawLegitimateResult.getInternalSkeletonCode(1);
        //there should still be one internal skeleton code mapped
        assertEquals(1, iscs.size());
        //check the values are unchanged
        assertTrue(iscs.containsKey(new Tuple<>(10,15)));
        HashSet<ITuple<Integer, Integer>> isc = iscs.get(new Tuple<>(10,15));
        assertEquals(1, isc.size());
        assertTrue(isc.contains(new Tuple<>(11,12)));

        //and check that the second files internal skeleton code is still empty
        iscs = rawLegitimateResult.getInternalSkeletonCode(2);
        assertEquals(0, iscs.size());
    }

    /**
     * Checking that when removing two internal skeleton code values, they are both added
     *  to the correct file's ISC map
     */
    @Test 
    void internalSCTwice() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25

        //removing 11-12 & 1-1 should affect first range, ie we should have:
        //internalFile1SkeletonCode: <10,15> = [<11,12>]
        //internalFile2SkeletonCode: Empty
        //locations/objects = <<16,20>,<21,25>>, <<10,15>,<13,20>>
        rawLegitimateResult.removeLine(new PairedTuple(11,12,1,1));
        //this has already been tested.

        //now removing 12-14 and 11 should change the code as such:
        //internalFile1SkeletonCode: <10,15> = [<11,12>,<12,14>] 
        //internalFile2SkeletonCode: Empty
        //locations/objects = <<10,15>,<13,20>>, <<16,20>,<21,25>>
        rawLegitimateResult.removeLine(new PairedTuple(12,14,11,11));

        //check that the two matches are unchanged
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        assertEquals(2, locations.size());
        PairedTuple<Integer, Integer, Integer, Integer> location;
        location = locations.get(0);
        assertEquals(10, location.getPoint1().getKey());
        assertEquals(15, location.getPoint1().getValue());
        assertEquals(13, location.getPoint2().getKey());
        assertEquals(20, location.getPoint2().getValue());
        location = locations.get(1);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(20, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(25, location.getPoint2().getValue());

        //and check that the objects are also unchanged
        List<NGramMatch> matches = rawLegitimateResult.getObjects();
        assertEquals(2, matches.size());
        ArrayList<ITuple<Integer, Integer>> matchLines;
        matchLines = matches.get(0).lines;
        assertEquals(10, matchLines.get(0).getKey());
        assertEquals(15, matchLines.get(0).getValue());
        assertEquals(13, matchLines.get(1).getKey());
        assertEquals(20, matchLines.get(1).getValue());
        matchLines = matches.get(1).lines;
        assertEquals(16, matchLines.get(0).getKey());
        assertEquals(20, matchLines.get(0).getValue());
        assertEquals(21, matchLines.get(1).getKey());
        assertEquals(25, matchLines.get(1).getValue());

        //then check internal skeleton code
        HashMap<ITuple,HashSet<ITuple<Integer, Integer>>> iscs = rawLegitimateResult.getInternalSkeletonCode(1);
        //there should be one map for the first file
        assertEquals(1, iscs.size());
        assertTrue(iscs.containsKey(new Tuple<>(10,15)));
        HashSet<ITuple<Integer, Integer>> isc = iscs.get(new Tuple<>(10,15));
        //and there should be two ranges of internal skeleton code associated for the range
        assertEquals(2, isc.size());
        //which should equal 11-12, and 12-14
        assertTrue(isc.contains(new Tuple<>(11,12)));
        assertTrue(isc.contains(new Tuple<>(12,14)));

        //and the second file should not have any internal skeleton code associated to it
        iscs = rawLegitimateResult.getInternalSkeletonCode(2);
        assertEquals(0, iscs.size());
    }

    /**
     * Testing that when a file has multiple internal skeleton code ranges that are connected
     *  that when removing a range that makes one ISC range external, both ISC ranges are removed.
     */
    @Test 
    void internalSCTwiceThenRemoveEdgeWithDoubleChange() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25

        //removing 11-12 & 1-1 should affect first range, ie we should have:
        //internalFile1SkeletonCode: <10,15> = [<11,12>]
        //internalFile2SkeletonCode: Empty
        //locations/objects = <<16,20>,<21,25>>, <<10,15>,<13,20>>
        // ie 11-12 is stored as internal skeleton code for file 1
        rawLegitimateResult.removeLine(new PairedTuple(11,12,1,1));
        //this has already been tested.

        //now removing 12-14 and 1-1 should change the code as such:
        //internalFile1SkeletonCode: <10,15> = [<11,12>,<12,14>] 
        //internalFile2SkeletonCode: Empty
        //locations/objects = <<10,15>,<13,20>>, <<16,20>,<21,25>>
        // ie both 11-12 and 12-14 is stored as ISC for file 1
        rawLegitimateResult.removeLine(new PairedTuple(12,14,11,11));
        //This has also already been tested

        //and now removing 10-10 and 1-1 should change the code as such:
        //internalFile1SkeletonCode: <10,15> = [<11,12>,<12,14>] 
        //internalFile2SkeletonCode: Empty
        //locations/objects = <<16,20>,<21,25>>,<<15,15>,<13,20>>
        //ie both 11-12 and 12-14 are removed as 11-12 becomes external, which when removed causes
        // 12-14 to also become external.

        rawLegitimateResult.removeLine(new PairedTuple(10,10,1,1));

        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        //first check that there are still two ranges
        assertEquals(2, locations.size());
        PairedTuple<Integer, Integer, Integer, Integer> location;
        //and check the second one has both 10-10, 11-12 and 12-14, removed from the first range
        location = locations.get(1);
        assertEquals(15, location.getPoint1().getKey());
        assertEquals(15, location.getPoint1().getValue());
        assertEquals(13, location.getPoint2().getKey());
        assertEquals(20, location.getPoint2().getValue());
        //check that the first range is unchanged
        location = locations.get(0);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(20, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(25, location.getPoint2().getValue());

        //check that the objects also are correct
        List<NGramMatch> matches = rawLegitimateResult.getObjects();
        assertEquals(2, matches.size());
        ArrayList<ITuple<Integer, Integer>> matchLines;
        matchLines = matches.get(1).lines;
        assertEquals(15, matchLines.get(0).getKey());
        assertEquals(15, matchLines.get(0).getValue());
        assertEquals(13, matchLines.get(1).getKey());
        assertEquals(20, matchLines.get(1).getValue());
        matchLines = matches.get(0).lines;
        assertEquals(16, matchLines.get(0).getKey());
        assertEquals(20, matchLines.get(0).getValue());
        assertEquals(21, matchLines.get(1).getKey());
        assertEquals(25, matchLines.get(1).getValue());

        //then check internal skeleton code
        HashMap<ITuple,HashSet<ITuple<Integer, Integer>>> iscs = rawLegitimateResult.getInternalSkeletonCode(1);
        //should have one range with internal skeleton code associated to it in file 1
        assertEquals(1, iscs.size());
        assertTrue(iscs.containsKey(new Tuple<>(10,15)));
        HashSet<ITuple<Integer, Integer>> isc = iscs.get(new Tuple<>(10,15));
        //check that there are two internal skeleton code ranges associated
        assertEquals(2, isc.size());
        assertTrue(isc.contains(new Tuple<>(11,12)));
        assertTrue(isc.contains(new Tuple<>(12,14)));

        // the second file should not have any ISC
        iscs = rawLegitimateResult.getInternalSkeletonCode(2);
        assertEquals(0, iscs.size());
    }

    /**
     * Testing that when a file has multiple internal skeleton code ranges that are connected
     *  that when removing a range that makes one ISC range external, both ISC ranges are removed. Checks that this
     *  works when the ranges are added backwards, ie a later range is added first.
     */
    @Test 
    void internalSCTwiceThenRemoveEdgeWithDoubleChangeInverted() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25

        //removing 12-14 and 1-1 should affect first range, ie we should have:
        //internalFile1SkeletonCode: <10,15> = [<12,14>] 
        //internalFile2SkeletonCode: Empty
        //locations/objects = <<10,15>,<13,20>>, <<16,20>,<21,25>>
        rawLegitimateResult.removeLine(new PairedTuple(12,14,1,1));
        //this has already been tested.

        //now removing 11-12 & 1-1 should change the code as such:
        //internalFile1SkeletonCode: <10,15> = [<12,14>,<11,12>] 
        //internalFile2SkeletonCode: Empty
        //locations/objects = <<10,15>,<13,20>>, <<16,20>,<21,25>>
        rawLegitimateResult.removeLine(new PairedTuple(11,12,11,11));

        //and now removing 10-10 and 1-1 should change the code as such:
        //internalFile1SkeletonCode: <10,15> = [<11,12>,<12,14>] 
        //internalFile2SkeletonCode: Empty
        //locations/objects = <<16,20>,<21,25>>,<<15,15>,<13,20>>

        rawLegitimateResult.removeLine(new PairedTuple(10,10,1,1));
        //checking the ranges have been removed correctly from the locations
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        assertEquals(2, locations.size());
        PairedTuple<Integer, Integer, Integer, Integer> location;
        location = locations.get(1);
        assertEquals(15, location.getPoint1().getKey());
        assertEquals(15, location.getPoint1().getValue());
        assertEquals(13, location.getPoint2().getKey());
        assertEquals(20, location.getPoint2().getValue());
        location = locations.get(0);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(20, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(25, location.getPoint2().getValue());

        //and checking that the match objects have also been updated correctly
        List<NGramMatch> matches = rawLegitimateResult.getObjects();
        assertEquals(2, matches.size());
        ArrayList<ITuple<Integer, Integer>> matchLines;
        matchLines = matches.get(1).lines;
        assertEquals(15, matchLines.get(0).getKey());
        assertEquals(15, matchLines.get(0).getValue());
        assertEquals(13, matchLines.get(1).getKey());
        assertEquals(20, matchLines.get(1).getValue());
        matchLines = matches.get(0).lines;
        assertEquals(16, matchLines.get(0).getKey());
        assertEquals(20, matchLines.get(0).getValue());
        assertEquals(21, matchLines.get(1).getKey());
        assertEquals(25, matchLines.get(1).getValue());

        //then check internal skeleton code
        HashMap<ITuple,HashSet<ITuple<Integer, Integer>>> iscs = rawLegitimateResult.getInternalSkeletonCode(1);
        //should have one map for the file
        assertEquals(1, iscs.size());
        assertTrue(iscs.containsKey(new Tuple<>(10,15)));
        HashSet<ITuple<Integer, Integer>> isc = iscs.get(new Tuple<>(10,15));
        //with two internal skeleton code ranges associated
        assertEquals(2, isc.size());
        assertTrue(isc.contains(new Tuple<>(11,12)));
        assertTrue(isc.contains(new Tuple<>(12,14)));

        //and check that the second file has no ISC stored.
        iscs = rawLegitimateResult.getInternalSkeletonCode(2);
        assertEquals(0, iscs.size());
    }
}