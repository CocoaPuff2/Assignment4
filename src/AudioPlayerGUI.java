package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.sound.sampled.*;
import org.apache.commons.math4.transform.*;
import org.jtransforms.fft.DoubleFFT_1D;
import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;  // You can choose other classifiers from WEKA
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class AudioPlayerGUI extends JFrame {
    private JList<String> fileList;
    private DefaultListModel<String> listModel;
    private JButton playButton, stopButton;
    private Clip currentClip;

    // for the ML file organization
    private ArrayList<String> allFiles = new ArrayList<>();
    private ArrayList<String> trainingFiles = new ArrayList<>();
    private ArrayList<String> testingFiles = new ArrayList<>();

    public AudioPlayerGUI() {
        // Set up JFrame
        setTitle("Audio Player");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create a list model and JList to display audio files
        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(fileList);

        // Create play and stop buttons
        playButton = new JButton("Play Selected File");
        stopButton = new JButton("Stop Audio");
        stopButton.setEnabled(false);  // Initially disable the stop button

        // Play button action
        playButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedFile = fileList.getSelectedValue();
                if (selectedFile != null) {
                    playAudio("audio/" + selectedFile);
                    extractFeatures("audio/" + selectedFile, selectedFile.startsWith("music/") ? "1" : "0");  // Convert label to numeric (1/0)
                } else {
                    JOptionPane.showMessageDialog(null, "Please select an audio file.");
                }
            }
        });

        // Stop button action
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopAudio();
            }
        });

        // Layout setup
        setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        panel.add(playButton);
        panel.add(stopButton);

        add(scrollPane, BorderLayout.CENTER);
        add(panel, BorderLayout.SOUTH);

        // Load audio files into the list model
        loadAudioFiles();
        // split the data files for testing and training
        splitdata();

        // Clear CSV and ARFF files on start
        clearFiles();
    }

    private void loadAudioFiles() {
        // Load music files
        File musicDir = new File("audio/music");
        File[] musicFiles = musicDir.listFiles((dir, name) -> name.endsWith(".wav"));
        if (musicFiles != null) {
            for (File file : musicFiles) {
                listModel.addElement("music/" + file.getName());
            }
        }

        // Load speech files
        File speechDir = new File("audio/speech");
        File[] speechFiles = speechDir.listFiles((dir, name) -> name.endsWith(".wav"));
        if (speechFiles != null) {
            for (File file : speechFiles) {
                listModel.addElement("speech/" + file.getName());
            }
        }
    }

    private void splitdata() {
        // Shuffles the list of files to ensure randomness
        Collections.shuffle(allFiles);

        // First 2/3 used for training
        int trainSize = (int) (allFiles.size() * 0.67);
        trainingFiles = new ArrayList<>(allFiles.subList(0, trainSize));

// Remaining 1/3 for testing
        testingFiles = new ArrayList<>(allFiles.subList(trainSize, allFiles.size()));
    }

    private void playAudio(String filePath) {
        try {
            File audioFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            currentClip = AudioSystem.getClip();  // Assign the Clip to the currentClip variable
            currentClip.open(audioStream);
            currentClip.start();
            stopButton.setEnabled(true);  // Enable the Stop button when the audio starts
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error playing the audio file.");
        }
    }

    private void stopAudio() {
        if (currentClip != null && currentClip.isRunning()) {
            currentClip.stop();
            currentClip.close();
            stopButton.setEnabled(false);  // Disable the Stop button after stopping the audio
        }
    }

    // The three fast fourier transformation features are:
    /*
   1.  Spectral Centroid: Measures the "center of mass" of the spectrum.
   2.  Spectral Bandwidth: Measures the width of the spectrum.
   3.  Spectral Roll Off: Measures the point where a certain percentage of the
       total spectral energy is contained.
     */
    private void extractFeatures(String filePath, String label) {
        try {
            File audioFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat format = audioStream.getFormat();
            int sampleRate = (int) format.getSampleRate();
            byte[] buffer = new byte[1024];
            int bytesRead;
            double rms = 0.0;
            int zeroCrossings = 0;
            int previousSample = 0;
            double maxAmplitude = 0.0;
            int totalSamples = 0; // To track number of samples
            ArrayList<Double> audioData = new ArrayList<>(); // To store the audio data for FFT

            // Check if audio is 16-bit or 8-bit
            boolean is16Bit = format.getSampleSizeInBits() == 16;

            // Process audio samples and extract features
            while ((bytesRead = audioStream.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    int sample = buffer[i];

                    if (is16Bit) {
                        // For 16-bit audio, combine two bytes to form a short sample
                        sample = (buffer[i] & 0xFF) | (buffer[i + 1] << 8);
                        i++;  // Skip the next byte
                    } else {
                        // For 8-bit audio, use the byte directly
                        sample = buffer[i];
                    }

                    // Amplitude (Max absolute value)
                    maxAmplitude = Math.max(maxAmplitude, Math.abs(sample));

                    // RMS Energy
                    rms += Math.pow(sample, 2);
                    totalSamples++; // counts number of samples

                    // Zero-Crossing Rate
                    if ((previousSample > 0 && sample <= 0) || (previousSample < 0 && sample >= 0)) {
                        zeroCrossings++;
                    }
                    previousSample = sample;
                }
            }

            // Calculate RMS value
            if (totalSamples > 0) {
                rms = Math.sqrt(rms / totalSamples);
            } else {
                rms = 0.0;
            }

            /*
            // Fast Fourier Transformations:
            // Convert audio data list to array for FFT
            double[] audioArray = audioData.stream().mapToDouble(Double::doubleValue).toArray();

            // FFT to extract the frequency-domain features
            DoubleFFT_1D fft = new DoubleFFT_1D(audioArray.length);
            fft.realForward(audioArray);
            //FastFourierTransformer transformer = new FastFourierTransformer(TransformType.FORWARD);
            // Complex[] fftResult = transformer.transform(audioArray);

            // Extract spectral features
            double spectralCentroid = calculateSpectralCentroid(audioArray);
            double spectralBandwidth = calculateSpectralBandwidth(audioArray, spectralCentroid);
            double spectralRolloff = calculateSpectralRolloff(audioArray, 0.85);  // 85% rolloff point
            /*
            double spectralCentroid = calculateSpectralCentroid(fftResult);
            double spectralBandwidth = calculateSpectralBandwidth(fftResult, spectralCentroid);
            double spectralRolloff = calculateSpectralRolloff(fftResult, 0.85);  // 85% rolloff point

             */

            // Print extracted features for debugging
            System.out.println("Features for " + filePath + ":");
            System.out.println("Max Amplitude: " + maxAmplitude);
            System.out.println("RMS Energy: " + rms);
            System.out.println("Zero-Crossing Rate: " + zeroCrossings);
            /*
            System.out.println("Spectral Centroid: " + spectralCentroid);
            System.out.println("Spectral Bandwidth: " + spectralBandwidth);
            System.out.println("Spectral Rolloff: " + spectralRolloff);

             */

            // Save features to CSV
            saveFeaturesToCSV(maxAmplitude, rms, zeroCrossings, label);

            // Convert CSV to ARFF after saving the features
            convertCsvToArff("features.csv", "features.arff");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error extracting features.");
        }
    }

    // Measures the spread of the spectrum around the centroid.
    private double calculateSpectralBandwidth(double[] spectrum, double centroid) {
        double sum = 0.0; // accumulate the weighted squared distances from the centroid.
        double totalMagnitude = 0.0; // of the spectrum

        // Iterate through the first half of the spectrum (FFT is symmetrical)
        for (int i = 0; i < spectrum.length / 2; i++) {
            double magnitude = Math.abs(spectrum[i]); // at current freq bin
            sum += magnitude * Math.pow(i - centroid, 2);
            totalMagnitude += magnitude;
        }

        // calc spectral bandwidth
        return totalMagnitude > 0 ? Math.sqrt(sum / totalMagnitude) : 0.0;
    }

    private double calculateSpectralRolloff(double[] spectrum, double rolloffPercentage) {
        double totalEnergy = 0.0;
        //  Calculate the total energy of the first half of the spectrum.
        for (int i = 0; i < spectrum.length / 2; i++) {
            totalEnergy += Math.abs(spectrum[i]);
        }

        double threshold = totalEnergy * rolloffPercentage; // energy threshold for the rolloff point
        double cumulativeEnergy = 0.0; // track the cumulative energy as we iterate through the spectrum.

        // iterate through first half of the spectrum
        for (int i = 0; i < spectrum.length / 2; i++) {
            cumulativeEnergy += Math.abs(spectrum[i]);

            // If cumulative energy meets or exceeds the threshold, return the current frequency bin index.
            if (cumulativeEnergy >= threshold) {
                return i; // Frequency bin
            }
        }

        return spectrum.length / 2 - 1; // Default to highest frequency bin if not reached
    }


    // Measures the "center of mass" of the spectrum.
    private double calculateSpectralCentroid(double[] spectrum) {
        double weightedSum = 0.0; //weighted sum of frequency magnitudes.
        double totalMagnitude = 0.0; // total magnitude of the spectrum.

        // iterates through the first half of the spectrum, since FFT output is symmetrical,
        // only need the FIRST HALF for spectral features.
        for (int i = 0; i < spectrum.length / 2; i++) { // Only half due to symmetry in FFT
            double magnitude = Math.abs(spectrum[i]); // at the current frequency bin.
            weightedSum += i * magnitude;
            totalMagnitude += magnitude;
        }

        // calc spectral centroid
        return totalMagnitude > 0 ? weightedSum / totalMagnitude : 0.0;
    }

    // Method to save the features to CSV
    private void saveFeaturesToCSV(double maxAmplitude, double rms, int zeroCrossings, String label) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("features.csv", true));  // Open CSV in append mode
            String data = maxAmplitude + "," + rms + "," + zeroCrossings + "," + label;
            writer.write(data);
            writer.newLine();  // Write data as new line
            writer.close();  // Close the writer
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error writing to CSV.");
        }
    }

    // Method to train the model using WEKA
    private void trainModel() {
        try {
            // Convert CSV to ARFF after saving the features
            convertCsvToArff("features.csv", "features.arff");

            // Load the ARFF file into WEKA
            DataSource source = new DataSource("features.arff");
            Instances data = source.getDataSet();
            // Set the class index to the last attribute (label)
            data.setClassIndex(data.numAttributes() - 1);

            // Train a classifier
            Classifier classifier = new J48();  // J48 is a decision tree classifier (you can use other classifiers)
            classifier.buildClassifier(data);

            // Save the classifier model
            weka.core.SerializationHelper.write("music_model.model", classifier);
            System.out.println("Model trained and saved.");

            // Test the model with the testing data
            testModel(classifier);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to test the model using WEKA
    private void testModel(Classifier classifier) {
        try {
            // Load the ARFF file
            DataSource source = new DataSource("features.arff");
            Instances data = source.getDataSet();
            data.setClassIndex(data.numAttributes() - 1);

            // header for the results
            System.out.println("#, Model Output, Ground Truth Label");

            // For testing, use the testing files
            for (String filePath : testingFiles) {
                // Extract features for the testing file (extract and test each file individually)
                double maxAmplitude = 0.0;
                double rms = 0.0;
                int zeroCrossings = 0;
                String label = filePath.startsWith("music/") ? "1" : "0"; // Label for the file (1 for music, 0 for speech)

                // extract features of the current file
                extractFeatures(filePath, label);
                
                // Create a new instance for the test file using the extracted features
                Instance testInstance = createTestInstance(maxAmplitude, rms, zeroCrossings, label, data);

                // Classify the instance using the trained classifier
                double prediction = classifier.classifyInstance(testInstance);

                // Output the prediction result
                String predictedClass = (prediction == 1.0) ? "Music" : "Speech"; // 1 = music, 0 = speech
                System.out.println("File: " + filePath + " | Predicted: " + predictedClass);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Instance createTestInstance(double maxAmplitude, double rms, int zeroCrossings, String label, Instances data) {
        // Create a new instance with the same number of attributes as the training data
        Instance instance = new DenseInstance(4);  // 4 attributes: maxAmplitude, rms, zeroCrossings, and label

        // Set the attribute values for the instance
        instance.setValue(0, maxAmplitude);        // Set amplitude value
        instance.setValue(1, rms);                 // Set RMS value
        instance.setValue(2, zeroCrossings);       // Set zero-crossing rate value
        instance.setValue(3, label.equals("1") ? 1.0 : 0.0);  // Set class label (1 for music, 0 for speech)

        // Set the instance's dataset (this is needed for classification)
        instance.setDataset(data);

        return instance;
    }

    // Method to convert CSV to ARFF
    private void convertCsvToArff(String csvFile, String arffFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile));
             BufferedWriter bw = new BufferedWriter(new FileWriter(arffFile))) {

            // Write ARFF header
            bw.write("@relation audio_features\n\n");
            bw.write("@attribute amplitude numeric\n");
            bw.write("@attribute rms numeric\n");
            bw.write("@attribute zero_crossing_rate numeric\n");
            bw.write("@attribute label {0, 1}\n\n");  // Convert yes/no to 0/1
            bw.write("@data\n");

            // Read and convert each line from CSV to ARFF
            String line;
            while ((line = br.readLine()) != null) {
                String[] columns = line.split(",");
                String amplitude = columns[0];
                String rms = columns[1];
                String zeroCrossingRate = columns[2];
                String label = columns[3];

                // Write to ARFF format (no file_name column)
                bw.write(amplitude + "," + rms + "," + zeroCrossingRate + "," + label + "\n");
            }

            System.out.println("CSV converted to ARFF successfully!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to clear CSV and ARFF files
    private void clearFiles() {
        File csvFile = new File("features.csv");
        File arffFile = new File("features.arff");

        if (csvFile.exists()) {
            csvFile.delete();  // Delete CSV file to clear data
        }

        if (arffFile.exists()) {
            arffFile.delete();  // Delete ARFF file to clear data
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                AudioPlayerGUI gui = new AudioPlayerGUI();
                gui.setVisible(true);
                // gui.trainModel();
            }
        });
    }
}
