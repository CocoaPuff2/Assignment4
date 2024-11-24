import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                AudioPlayerGUI gui = new AudioPlayerGUI();
                gui.setVisible(true);
            }
        });
    }
}
