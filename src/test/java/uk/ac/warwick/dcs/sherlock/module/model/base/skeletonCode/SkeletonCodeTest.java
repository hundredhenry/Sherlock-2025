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

class SkeletonCodeTest {

    private EntityFile skeletonFile;
    private EntityFile testFile1;
    private EntityFile testFile2;
    private NGramRawResult<NGramMatch> rawLegitimateResult;

    @BeforeEach
    void setUp() {
        // EntityWorkspace testWorkspace = new EntityWorkspace("Test", "Java");
        EntityArchive testArchive = new EntityArchive("TestArchive");
        testFile1 = new EntityFile(
            testArchive, "TestFile.java", "java", new Timestamp(1), 50, 50, 50
        );

        testFile2 = new EntityFile(
            testArchive, "TestFile2.java", "java", new Timestamp(1), 50, 50, 50
        );

        EntityArchive skeletonArchive = new EntityArchive("TestSkeletonArchive");
        skeletonFile = new EntityFile(
            skeletonArchive, "TestSkeletonFile.java", "java", new Timestamp(1), 50, 50, 50
        );

        rawLegitimateResult = new NGramRawResult<>(testFile1, testFile2);

        NGramMatch match;
        match = new NGramMatch(10, 15, 13, 20, 0.5f, testFile1, testFile2);
        rawLegitimateResult.put(match, 10,15,13,20);
        match = new NGramMatch(16, 20, 21,25, 0.5f, testFile1, testFile2);
        rawLegitimateResult.put(match, 16,20,21,25);
        

    }

    @AfterEach
    void tearDown() {
    }


    @Test
    void lineRemovalOutsideOfRange() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25
        //removing 1,1 shouldnt affect anything (other than reversing the results)
        rawLegitimateResult.removeLine(new PairedTuple<>(1,1,1,1));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
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
    }

    @Test
    void lineRemovalFullyEncompassesAllRanges() {
        rawLegitimateResult.removeLine(new PairedTuple<>(1,30,1,30));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        assertEquals(0, locations.size());

        List<NGramMatch> matches = rawLegitimateResult.getObjects();
        assertEquals(0, matches.size());
    }

    @Test
    void lineRemovalFullyEncompassesOneRange() {
        rawLegitimateResult.removeLine(new PairedTuple<>(10,15,13,20));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        assertEquals(1, locations.size());
        PairedTuple<Integer, Integer, Integer, Integer> location = locations.get(0);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(20, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(25, location.getPoint2().getValue());

        List<NGramMatch> matches = rawLegitimateResult.getObjects();
        assertEquals(1, matches.size());
        ArrayList<ITuple<Integer, Integer>> matchLines = matches.get(0).lines;
        assertEquals(16, matchLines.get(0).getKey());
        assertEquals(20, matchLines.get(0).getValue());
        assertEquals(21, matchLines.get(1).getKey());
        assertEquals(25, matchLines.get(1).getValue());
    }

    @Test
    void lineRemovalPartiallyEncompassesOneRange() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25 
        //removing 1-15 & 1-1, should fully remove the first range
        rawLegitimateResult.removeLine(new PairedTuple<>(10,15,1,1));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        assertEquals(1, locations.size());
        PairedTuple<Integer, Integer, Integer, Integer> location = locations.get(0);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(20, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(25, location.getPoint2().getValue());

        List<NGramMatch> matches = rawLegitimateResult.getObjects();
        assertEquals(1, matches.size());
        ArrayList<ITuple<Integer, Integer>> matchLines = matches.get(0).lines;
        assertEquals(16, matchLines.get(0).getKey());
        assertEquals(20, matchLines.get(0).getValue());
        assertEquals(21, matchLines.get(1).getKey());
        assertEquals(25, matchLines.get(1).getValue());
    }

    @Test 
    void lineRemovalPartiallyEncompassesTwoRanges() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25 
        //removing 10-15 & 21-25, should fully remove both ranges
        rawLegitimateResult.removeLine(new PairedTuple<>(10,15,21,25));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        assertEquals(0, locations.size());

        List<NGramMatch> matches = rawLegitimateResult.getObjects();
        assertEquals(0, matches.size());
    }

    @Test 
    void lineRemovalTrimsOneLeftBoundary() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25
        //removing 8-12 & 1-1 should affect first range, setting it to 13-15 & 13-20, but not affecting second range
        rawLegitimateResult.removeLine(new PairedTuple<>(8,12,1,1));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        assertEquals(2, locations.size());
        PairedTuple<Integer, Integer, Integer, Integer> location;
        location = locations.get(1);
        assertEquals(13, location.getPoint1().getKey());
        assertEquals(15, location.getPoint1().getValue());
        assertEquals(13, location.getPoint2().getKey());
        assertEquals(20, location.getPoint2().getValue());
        location = locations.get(0);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(20, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(25, location.getPoint2().getValue());

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
    
    void lineRemovalTrimsBothLeftBoundaries() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25
        //removing 8-12 & 11-15 should affect both ranges, setting them to 13-15 & 16-20, but not affecting second range
        rawLegitimateResult.removeLine(new PairedTuple<>(8,12,11,15));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        assertEquals(2, locations.size());
        PairedTuple<Integer, Integer, Integer, Integer> location;
        location = locations.get(1);
        assertEquals(13, location.getPoint1().getKey());
        assertEquals(15, location.getPoint1().getValue());
        assertEquals(16, location.getPoint2().getKey());
        assertEquals(20, location.getPoint2().getValue());
        location = locations.get(0);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(20, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(25, location.getPoint2().getValue());

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

    @Test 
    void lineRemovalTrimsOneRightBoundary() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25
        //removing 18-21 & 1-1 should affect second range, setting it to 16-17 & 21-25, but not affecting first range
        rawLegitimateResult.removeLine(new PairedTuple<>(18,21,1,1));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        assertEquals(2, locations.size());
        PairedTuple<Integer, Integer, Integer, Integer> location;
        location = locations.get(1);
        assertEquals(10, location.getPoint1().getKey());
        assertEquals(15, location.getPoint1().getValue());
        assertEquals(13, location.getPoint2().getKey());
        assertEquals(20, location.getPoint2().getValue());
        location = locations.get(0);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(17, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(25, location.getPoint2().getValue());

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

    @Test 
    void lineRemovalTrimsBothRightBoundaries() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25
        //removing 18-21 & 23-26 should affect both ranges, setting them to 16-17 & 21-22, but not affecting first range
        rawLegitimateResult.removeLine(new PairedTuple<>(18,21,23,26));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        assertEquals(2, locations.size());
        PairedTuple<Integer, Integer, Integer, Integer> location;
        location = locations.get(1);
        assertEquals(10, location.getPoint1().getKey());
        assertEquals(15, location.getPoint1().getValue());
        assertEquals(13, location.getPoint2().getKey());
        assertEquals(20, location.getPoint2().getValue());
        location = locations.get(0);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(17, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(22, location.getPoint2().getValue());
        
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

    @Test
    void lineRemovalWithinOneRangeOneFile() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25
        //removing 11-12 & 1-1 should affect first range, ie we should have:

        //internalFile1SkeletonCode: <10,15> = [<11,12>]
        //internalFile2SkeletonCode: Empty
        //locations/objects = <<16,20>,<21,25>>, <<10,15>,<13,20>>
        rawLegitimateResult.removeLine(new PairedTuple(11,12,1,1));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
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
        assertEquals(1, iscs.size());
        // HashSet<ITuple<Integer, Integer>> temp = new HashSet<>();
        ITuple<Integer, Integer> key = new Tuple<>(10,15);
        assertTrue(iscs.containsKey(key));
        HashSet<ITuple<Integer, Integer>> isc = iscs.get(key);
        assertEquals(1, isc.size());
        assertTrue(isc.contains(new Tuple<>(11,12)));

        iscs = rawLegitimateResult.getInternalSkeletonCode(2);
        assertEquals(0, iscs.size());

    }

    @Test
    void lineRemovalWithinOneRangeTwoFiles() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25
        //removing 11-12 & 15-17 should mean that we have:

        //internalFile1SkeletonCode: <10,15> = [<11,12>]
        //internalFile2SkeletonCode: <13,20> = [<15,17>]
        //locations/objects = <<16,20>,<21,25>>, <<10,15>,<13,20>>
        rawLegitimateResult.removeLine(new PairedTuple(11,12,15,17));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
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
        assertEquals(1, iscs.size());
        // HashSet<ITuple<Integer, Integer>> temp = new HashSet<>();
        ITuple<Integer,Integer> key = new Tuple<>(10,15);
        assertTrue(iscs.containsKey(key));
        HashSet<ITuple<Integer, Integer>> isc = iscs.get(key);
        assertEquals(1, isc.size());
        assertTrue(isc.contains(new Tuple<>(11,12)));

        iscs = rawLegitimateResult.getInternalSkeletonCode(2);
        assertEquals(1, iscs.size());
        // temp = new HashSet<>();
        key=new Tuple<>(13,20);
        assertTrue(iscs.containsKey(key));
        isc = iscs.get(key);
        assertEquals(1, isc.size());
        assertTrue(isc.contains(new Tuple<>(15,17)));
    }

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
        assertEquals(2, iscs.size());
        assertTrue(iscs.containsKey(new Tuple<>(10,13)));
        assertTrue(iscs.containsKey(new Tuple<>(10,15)));
        HashSet<ITuple<Integer, Integer>> isc = iscs.get(new Tuple<>(10,13));
        assertEquals(1, isc.size());
        assertTrue(isc.contains(new Tuple<>(11,12)));
        isc = iscs.get(new Tuple<>(10,15));
        assertEquals(1, isc.size());
        assertTrue(isc.contains(new Tuple<>(11,12)));

        iscs = rawLegitimateResult.getInternalSkeletonCode(2);
        assertEquals(0, iscs.size());
    }

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
        rawLegitimateResult.removeLine(new PairedTuple(10,10,1,1));
        List<PairedTuple<Integer, Integer, Integer, Integer>> locations = rawLegitimateResult.getLocations();
        assertEquals(2, locations.size());
        PairedTuple<Integer, Integer, Integer, Integer> location;
        location = locations.get(0);
        assertEquals(13, location.getPoint1().getKey());
        assertEquals(15, location.getPoint1().getValue());
        assertEquals(13, location.getPoint2().getKey());
        assertEquals(20, location.getPoint2().getValue());
        location = locations.get(1);
        assertEquals(16, location.getPoint1().getKey());
        assertEquals(20, location.getPoint1().getValue());
        assertEquals(21, location.getPoint2().getKey());
        assertEquals(25, location.getPoint2().getValue());

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
        assertEquals(1, iscs.size());
        assertTrue(iscs.containsKey(new Tuple<>(10,15)));
        HashSet<ITuple<Integer, Integer>> isc = iscs.get(new Tuple<>(10,15));
        assertEquals(1, isc.size());
        assertTrue(isc.contains(new Tuple<>(11,12)));

        iscs = rawLegitimateResult.getInternalSkeletonCode(2);
        assertEquals(0, iscs.size());
    }

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
        assertEquals(1, iscs.size());
        assertTrue(iscs.containsKey(new Tuple<>(10,15)));
        HashSet<ITuple<Integer, Integer>> isc = iscs.get(new Tuple<>(10,15));
        assertEquals(2, isc.size());
        assertTrue(isc.contains(new Tuple<>(11,12)));
        assertTrue(isc.contains(new Tuple<>(12,14)));

        iscs = rawLegitimateResult.getInternalSkeletonCode(2);
        assertEquals(0, iscs.size());
    }

    @Test 
    void internalSCTwiceThenRemoveEdgeWithDoubleChange() {
        //inside the code we have ranges of 10-15 & 13-20, and 16-20 & 21-25
        //removing 11-12 & 1-1 should affect first range, ie we should have:

        //internalFile1SkeletonCode: <10,15> = [<11,12>]
        //internalFile2SkeletonCode: Empty
        //locations/objects = <<16,20>,<21,25>>, <<10,15>,<13,20>>
        rawLegitimateResult.removeLine(new PairedTuple(11,12,1,1));
        //this has already been tested.

        //now removing 12-14 and 1-1 should change the code as such:

        //internalFile1SkeletonCode: <10,15> = [<11,12>,<12,14>] 
        //internalFile2SkeletonCode: Empty
        //locations/objects = <<10,15>,<13,20>>, <<16,20>,<21,25>>
        rawLegitimateResult.removeLine(new PairedTuple(12,14,11,11));

        //and now removing 10-10 and 1-1 should change the code as such:

        //internalFile1SkeletonCode: <10,15> = [<11,12>,<12,14>] 
        //internalFile2SkeletonCode: Empty
        //locations/objects = <<16,20>,<21,25>>,<<15,15>,<13,20>>

        rawLegitimateResult.removeLine(new PairedTuple(10,10,1,1));

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
        assertEquals(1, iscs.size());
        assertTrue(iscs.containsKey(new Tuple<>(10,15)));
        HashSet<ITuple<Integer, Integer>> isc = iscs.get(new Tuple<>(10,15));
        assertEquals(2, isc.size());
        assertTrue(isc.contains(new Tuple<>(11,12)));
        assertTrue(isc.contains(new Tuple<>(12,14)));

        iscs = rawLegitimateResult.getInternalSkeletonCode(2);
        assertEquals(0, iscs.size());
    }

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
        assertEquals(1, iscs.size());
        assertTrue(iscs.containsKey(new Tuple<>(10,15)));
        HashSet<ITuple<Integer, Integer>> isc = iscs.get(new Tuple<>(10,15));
        assertEquals(2, isc.size());
        assertTrue(isc.contains(new Tuple<>(11,12)));
        assertTrue(isc.contains(new Tuple<>(12,14)));

        iscs = rawLegitimateResult.getInternalSkeletonCode(2);
        assertEquals(0, iscs.size());
    }
}