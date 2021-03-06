package com.davesnowdon.devoxxuk;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.deeplearning4j.datasets.iterator.impl.CifarDataSetIterator;
import org.deeplearning4j.examples.utilities.DataUtilities;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.net.URL;

/*
 * This classes exists to causes gradle to download dependencies and, when run, preloads data files required for the
 * other exercises.
 */
public class DataLoader {
    public static final String DATA_VAR = "DEVOXXUK_GHDDL_DATA";
    public static final String DATA_DIR = System.getenv(DATA_VAR);
    public static final String DATA_URL = "http://ai.stanford.edu/~amaas/data/sentiment/aclImdb_v1.tar.gz";
    public static final String GOOGLE_NEWS_VECTOR_ARCHIVE_URL = "https://s3.amazonaws.com/dl4j-distribution/GoogleNews-vectors-negative300.bin.gz";
    public static final String EMBEDDINGS_FILE = "GoogleNews-vectors-negative300.bin.gz";


    public static void main(String[] args) throws Exception {
        if (null == DATA_DIR) {
            System.err.println("Please set the environment variable: " + DATA_VAR + " to the directory where you wish to store data files");
            System.exit(1);
        }

        checkEmbeddings();

        final String sentimentDataDir = FilenameUtils.concat(DATA_DIR, "dl4j_w2vSentiment/");
        downloadReviewData(sentimentDataDir);

        downloadCifar();

        // Check we can create instances of ND4J and DL4J classes
        checkDl4j();

        System.out.println("\nAll good");
        System.exit(0);
    }

    private static void checkEmbeddings() throws Exception {
        final File embeddingsFile = new File(DATA_DIR, EMBEDDINGS_FILE);
        if (!embeddingsFile.exists()) {
            System.err.println("Downloading the 1.5Gb file " + EMBEDDINGS_FILE + " and placing in" + DATA_DIR);
            downloadGoogleNewsVectorArchive(DATA_DIR);
        } else {
            //Assume if archive (.tar.gz) exists, then data has already been extracted
            System.out.println("Data (.bin.gz file) already exists at " + embeddingsFile.getAbsolutePath());
        }
    }

    private static void downloadGoogleNewsVectorArchive(final String dataDir) throws Exception {
        //Create directory if required
        File directory = new File(dataDir);
        if (!directory.exists()) directory.mkdir();

        //Download file:
        String archizePath = dataDir + "GoogleNews-vectors-negative300.bin.gz";
        File archiveFile = new File(archizePath);

        System.out.println("Starting data download (1.5GB)...");
        FileUtils.copyURLToFile(new URL(GOOGLE_NEWS_VECTOR_ARCHIVE_URL), archiveFile);
        System.out.println("Data (.bin.gz file) downloaded to " + archiveFile.getAbsolutePath());
    }

    private static void downloadReviewData(final String dataDir) throws Exception {
        //Create directory if required
        File directory = new File(dataDir);
        if (!directory.exists()) directory.mkdir();

        //Download file:
        String archizePath = dataDir + "aclImdb_v1.tar.gz";
        File archiveFile = new File(archizePath);
        String extractedPath = dataDir + "aclImdb";
        File extractedFile = new File(extractedPath);

        if (!archiveFile.exists()) {
            System.out.println("Starting data download (80MB)...");
            FileUtils.copyURLToFile(new URL(DATA_URL), archiveFile);
            System.out.println("Data (.tar.gz file) downloaded to " + archiveFile.getAbsolutePath());
            //Extract tar.gz file to output directory
            DataUtilities.extractTarGz(archizePath, dataDir);
        } else {
            //Assume if archive (.tar.gz) exists, then data has already been extracted
            System.out.println("Data (.tar.gz file) already exists at " + archiveFile.getAbsolutePath());
            if (!extractedFile.exists()) {
                //Extract tar.gz file to output directory
                DataUtilities.extractTarGz(archizePath, dataDir);
            } else {
                System.out.println("Data (extracted) already exists at " + extractedFile.getAbsolutePath());
            }
        }
    }

    private static void downloadCifar() throws Exception {
        System.out.println("\nDownloading the CIFAR10 dataset. This will create a directory called 'cifar' in your home directory");

        System.out.println("\nYou may now see some warnings and stacktraces generated by DL4J - please ignore\n\n");

        CifarDataSetIterator cifar = new CifarDataSetIterator(100, 50000,
                new int[]{32, 32, 3}, false, true);
    }

    private static void checkDl4j() {
        System.out.println("\nYou may now see some warnings and stacktraces generated by DL4J - please ignore\n\n");

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(42)
                .updater(Updater.ADAM)  //To configure: .updater(Adam.builder().beta1(0.9).beta2(0.999).build())
                .regularization(true).l2(1e-5)
                .weightInit(WeightInit.XAVIER)
                .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue).gradientNormalizationThreshold(1.0)
                .learningRate(2e-2)
                .trainingWorkspaceMode(WorkspaceMode.SEPARATE).inferenceWorkspaceMode(WorkspaceMode.SEPARATE)   //https://deeplearning4j.org/workspaces
                .list()
                .layer(0, new GravesLSTM.Builder().nIn(1024).nOut(256)
                        .activation(Activation.TANH).build())
                .layer(1, new RnnOutputLayer.Builder().activation(Activation.SOFTMAX)
                        .lossFunction(LossFunctions.LossFunction.MCXENT).nIn(256).nOut(2).build())
                .pretrain(false).backprop(true).build();
        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();

        INDArray weights0 = Nd4j.randn(10, 10);
    }
}
