package dev.esophose.playerparticles.util.inputparser.parsable;

import dev.esophose.playerparticles.particles.PPlayer;
import dev.esophose.playerparticles.particles.ParticleEffect.NoteColor;
import dev.esophose.playerparticles.util.inputparser.Parsable;
import java.util.List;

public class ParsableNoteColor extends Parsable<NoteColor> {

    public ParsableNoteColor() {
        super(NoteColor.class);
    }

    @Override
    public NoteColor parse(PPlayer pplayer, List<String> inputs) {
        String input = inputs.remove(0);
        if (input.equalsIgnoreCase("rainbow")) {
            return new NoteColor(99);
        } else if (input.equalsIgnoreCase("random")) {
            return new NoteColor(98);
        }

        int note = Integer.parseInt(input);
        return new NoteColor(note);
    }

}