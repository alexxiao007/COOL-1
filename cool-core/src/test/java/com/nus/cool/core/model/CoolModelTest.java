package com.nus.cool.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nus.cool.core.cohort.ExtendedCohortQuery;
import com.nus.cool.core.cohort.funnel.FunnelQuery;
import com.nus.cool.core.io.readstore.CubeRS;
import com.nus.cool.core.io.storevector.InputVector;
import com.nus.cool.core.util.config.CsvDataLoaderConfig;
import com.nus.cool.core.util.config.DataLoaderConfig;
import com.nus.cool.result.ExtendedResultTuple;
import com.nus.cool.model.CoolCohortEngine;
import com.nus.cool.model.CoolLoader;
import com.nus.cool.model.CoolModel;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CoolModelTest {

    @Test(priority = 0)
    public static void CsvLoaderTest() {
        System.out.println("======================== Csv data loader Test ========================");
        // System.out.println(System.getProperty("user.dir"));
        String cube = "health";
        String schemaFileName = "../health/table.yaml";
        String dimFileName = "../health/dim2.csv";
        String dataFileName = "../health/raw2.csv";
        String cubeRepo = "../datasetSource";

        DataLoaderConfig config = new CsvDataLoaderConfig();

        try{
            CoolLoader loader = new CoolLoader(config);
            loader.load(cube, schemaFileName, dimFileName, dataFileName, cubeRepo);
        } catch (IOException e){
            System.out.println(e);
        }
    }

    @Test (priority = 10)
    public void CubeListTest() {
        System.out.println("======================== Cube List Test ========================");
        // System.out.println(System.getProperty("user.dir"));
        String datasetPath = "../datasetSource";
        try {
            CoolModel model = new CoolModel(datasetPath);
            String[] cubes2 = model.listCubes();
            System.out.println("Applications: " + Arrays.toString(cubes2));
        } catch (IOException e){
            System.out.println(e);
        }
    }

    @Test (priority = 1)
    public static void CubeReloadTest() {
        System.out.println("======================== Cube Reload Test ========================");
        String datasetPath = "../datasetSource";
        String queryPath = "../health/query1-0.json";

        try {
            ObjectMapper mapper = new ObjectMapper();
            ExtendedCohortQuery query = mapper.readValue(new File(queryPath), ExtendedCohortQuery.class);

            String inputSource = query.getDataSource();
            CoolModel coolModel = new CoolModel(datasetPath);
            coolModel.reload(inputSource);
            System.out.println(coolModel);
        } catch (IOException e){
            System.out.println(e);
        }
    }

    @Test (priority = 2)
    public void CohortSelectionTest() {
        System.out.println("======================== Cohort Selection Test ========================");
        String datasetPath = "../datasetSource";
        String queryPath = "../health/query1-0.json";

        try{
            ObjectMapper mapper = new ObjectMapper();
            ExtendedCohortQuery query = mapper.readValue(new File(queryPath), ExtendedCohortQuery.class);

            String inputSource = query.getDataSource();
            CoolModel coolModel = new CoolModel(datasetPath);
            coolModel.reload(inputSource);

            CubeRS cube = coolModel.getCube(query.getDataSource());

            List<Integer> cohorResults = coolModel.cohortEngine.selectCohortUsers(cube,null, query);
            System.out.println("Result for query is  " + cohorResults);
            List<String> userIDs = coolModel.cohortEngine.listCohortUsers(cube, cohorResults);
            System.out.println("Actual user IDs are  " + userIDs);

            String outputCohort = query.getOutputCohort();
            File cohortRoot =  new File(coolModel.getCubeStorePath(inputSource), "cohort");
            if(!cohortRoot.exists()){
                cohortRoot.mkdir();
                System.out.println("[*] Cohort Fold " + cohortRoot.getName() + " is created.");
            }
            File cohortFile = new File(cohortRoot, outputCohort);
            if (cohortFile.exists()){
                cohortFile.delete();
                System.out.println("[*] Cohort " + outputCohort + " exists and is deleted!");
            }

            coolModel.cohortEngine.createCohort(query, cohorResults, cohortRoot);
            System.out.println("[*] Cohort results are stored into " + cohortRoot.getAbsolutePath());
        } catch (IOException e){
            System.out.println(e);
        }
    }

    @Test(priority = 3)
    public void CohortAnalysis(){
        System.out.println("======================== Cohort Analysis Test ========================");
        String datasetPath = "../datasetSource";
        String queryPath = "../health/query2.json";

        try {
            ObjectMapper mapper = new ObjectMapper();
            ExtendedCohortQuery query = mapper.readValue(new File(queryPath), ExtendedCohortQuery.class);

            String inputSource = query.getDataSource();
            CoolModel coolModel = new CoolModel(datasetPath);
            coolModel.reload(inputSource);

            if (!query.isValid())
                throw new IOException("[x] Invalid cohort query.");

            CubeRS inputCube = coolModel.getCube(query.getDataSource());
            String inputCohort = query.getInputCohort();
            if (inputCohort != null) {
                System.out.println("Input cohort: " + inputCohort);
                coolModel.loadCohorts(inputCohort, inputSource);
            }
            InputVector userVector = coolModel.getCohortUsers(inputCohort);
            List<ExtendedResultTuple> result = coolModel.cohortEngine.performCohortQuery(inputCube, userVector, query);
            System.out.println("Result for the query is  " + result);
        } catch (IOException e){
            System.out.println(e);
        }
    }

     @Test(priority = 4)
    public void FunnelAnalysis(){
        System.out.println("======================== Funnel Analysis Test ========================");
        String datasetPath = "../datasetSource";
        String queryPath = "../sogamo/query1.json";

        try {
            ObjectMapper mapper = new ObjectMapper();
            FunnelQuery query = mapper.readValue(new File(queryPath), FunnelQuery.class);

            String inputSource = query.getDataSource();
            CoolModel coolModel = new CoolModel(datasetPath);
            coolModel.reload(inputSource);

            if (!query.isValid())
                throw new IOException("[x] Invalid cohort query.");

            CubeRS inputCube = coolModel.getCube(query.getDataSource());
            String inputCohort = query.getInputCohort();
            if (inputCohort != null) {
                System.out.println("Input cohort: " + inputCohort);
                coolModel.loadCohorts(inputCohort, inputSource);
            }
            InputVector userVector = coolModel.getCohortUsers(inputCohort);
            int[] result = coolModel.cohortEngine.performFunnelQuery(inputCube, userVector, query);
            System.out.println("Result for the query is  " + Arrays.toString(result));
        } catch (IOException e){
            System.out.println(e);
        }
    }
}
