package pcd.ass01;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.*;
import java.util.Hashtable;

public class BoidsView implements ChangeListener {

	private final JFrame frame;
	private final BoidsPanel boidsPanel;
	private final JSlider cohesionSlider, separationSlider, alignmentSlider;
	private final BoidsModel model;
	private final int width, height;
    private final BoidsSimulator simulator;

	public BoidsView(final BoidsModel model, final int width, final int height, final BoidsSimulator simulator) {
		this.model = model;
		this.width = width;
		this.height = height;
        this.simulator = simulator;
		
		frame = new JFrame("Boids Simulation");
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel cp = new JPanel();
		LayoutManager layout = new BorderLayout();
		cp.setLayout(layout);

        JPanel inputPanel = getInputePanel(model);
        cp.add(BorderLayout.NORTH, inputPanel);

        boidsPanel = new BoidsPanel(this, model);
		cp.add(BorderLayout.CENTER, boidsPanel);

        JPanel slidersPanel = new JPanel();
        
        cohesionSlider = makeSlider();
        separationSlider = makeSlider();
        alignmentSlider = makeSlider();
        
        slidersPanel.add(new JLabel("Separation"));
        slidersPanel.add(separationSlider);
        slidersPanel.add(new JLabel("Alignment"));
        slidersPanel.add(alignmentSlider);
        slidersPanel.add(new JLabel("Cohesion"));
        slidersPanel.add(cohesionSlider);
		        
		cp.add(BorderLayout.SOUTH, slidersPanel);

		frame.setContentPane(cp);	
		
        frame.setVisible(true);
	}

	private JSlider makeSlider() {
		var slider = new JSlider(JSlider.HORIZONTAL, 0, 20, 10);        
		slider.setMajorTickSpacing(10);
		slider.setMinorTickSpacing(1);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		var labelTable = new Hashtable<>();
		labelTable.put( 0, new JLabel("0") );
		labelTable.put( 10, new JLabel("1") );
		labelTable.put( 20, new JLabel("2") );
		slider.setLabelTable( labelTable );
		slider.setPaintLabels(true);
        slider.addChangeListener(this);
		return slider;
	}

    private JPanel getInputePanel(final BoidsModel model) {
        JPanel inputPanel = new JPanel();
        var boidsNumberInput = new JTextField(String.valueOf(model.getBoids().size()), 10);
        boidsNumberInput.addActionListener((_) -> {
			if (boidsNumberInput.isEditable()) {
				boidsNumberInput.setEditable(false);
			}
        });
        var resumeButton = getJButton(boidsNumberInput);
        var stopButton = new JButton("RESUME/STOP");
        stopButton.addActionListener((_) -> {
            if (!boidsNumberInput.isEditable() && this.model.boidsHaveBeenSet()) {
                this.simulator.resumeStop();
            }
        });

        var resetButton = new JButton("RESET");
        resetButton.addActionListener((_) -> {
            if (!boidsNumberInput.isEditable() && this.model.boidsHaveBeenSet()) {
				boidsNumberInput.setEditable(true);
				boidsNumberInput.setText("0");
                this.simulator.reset();
				this.boidsPanel.repaint();
            }
        });

        inputPanel.add(resumeButton);
        inputPanel.add(stopButton);
		inputPanel.add(resetButton);
        inputPanel.add(boidsNumberInput);
        return inputPanel;
    }

    private JButton getJButton(JTextField boidsNumberInput) {
        var resumeButton = new JButton("START");
        resumeButton.addActionListener((_) -> {
            if (!boidsNumberInput.isEditable()) {
                if (!this.model.boidsHaveBeenSet()) {
					try { 
                   		this.model.setBoids(Integer.parseInt(boidsNumberInput.getText()));
						this.simulator.start();
					} catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(frame, "Inserisci un numero valido.", "Errore", JOptionPane.ERROR_MESSAGE);
                        boidsNumberInput.setEditable(true);
						return;
                    };
                }
            }
        });
        return resumeButton;
    }

    public void update(final int frameRate) {
        boidsPanel.setFrameRate(frameRate);
		boidsPanel.repaint();
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == separationSlider) {
			var val = separationSlider.getValue();
			model.setSeparationWeight(0.1*val);
		} else if (e.getSource() == cohesionSlider) {
			var val = cohesionSlider.getValue();
			model.setCohesionWeight(0.1*val);
		} else {
			var val = alignmentSlider.getValue();
			model.setAlignmentWeight(0.1*val);
		}
	}
	
	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

}
