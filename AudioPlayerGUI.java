import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.sound.sampled.*;

public class AudioPlayerGUI extends JFrame {
    private JList<String> fileList;
    private DefaultListModel<String> listModel;
    private JButton playButton, stopButton;
    private Clip currentClip;

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

            // Print extracted features for debugging
            System.out.println("Features for " + filePath + ":");
            System.out.println("Max Amplitude: " + maxAmplitude);
            System.out.println("RMS Energy: " + rms);
            System.out.println("Zero-Crossing Rate: " + zeroCrossings);

            // Save features to CSV
            saveFeaturesToCSV(maxAmplitude, rms, zeroCrossings, label);

            // Convert CSV to ARFF after saving the features
            convertCsvToArff("features.csv", "features.arff");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error extracting features.");
        }
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
            }
        });
    }
}
