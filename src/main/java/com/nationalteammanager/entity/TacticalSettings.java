package com.nationalteammanager.entity;

import com.nationalteammanager.enums.AttackDirection;
import com.nationalteammanager.enums.DefensiveLine;
import com.nationalteammanager.enums.Mentality;
import com.nationalteammanager.enums.PassingStyle;
import com.nationalteammanager.enums.PlayStyle;
import com.nationalteammanager.enums.PlayTempo;
import com.nationalteammanager.enums.PlayWidth;
import com.nationalteammanager.enums.PressingIntensity;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Taktičke postavke jednog sastava. Klasa je @Embeddable, pa su njena polja kolone tabele Lineups:
 * svaki sastav ima tačno jednu taktiku i za to nije potrebna posebna tabela.
 */
@Embeddable
public class TacticalSettings {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlayStyle playStyle = PlayStyle.POSSESSION;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Mentality mentality = Mentality.BALANCED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlayWidth playWidth = PlayWidth.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlayTempo playTempo = PlayTempo.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PressingIntensity pressing = PressingIntensity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DefensiveLine defensiveLine = DefensiveLine.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PassingStyle passingStyle = PassingStyle.MIXED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttackDirection attackDirection = AttackDirection.THROUGH_MIDDLE;

    protected TacticalSettings() {
    }

    public TacticalSettings(PlayStyle playStyle, Mentality mentality, PlayWidth playWidth, PlayTempo playTempo,
                            PressingIntensity pressing, DefensiveLine defensiveLine, PassingStyle passingStyle,
                            AttackDirection attackDirection) {
        this.playStyle = playStyle;
        this.mentality = mentality;
        this.playWidth = playWidth;
        this.playTempo = playTempo;
        this.pressing = pressing;
        this.defensiveLine = defensiveLine;
        this.passingStyle = passingStyle;
        this.attackDirection = attackDirection;
    }

    public static TacticalSettings defaults() {
        return new TacticalSettings();
    }

    public PlayStyle getPlayStyle() {
        return playStyle;
    }

    public Mentality getMentality() {
        return mentality;
    }

    public PlayWidth getPlayWidth() {
        return playWidth;
    }

    public PlayTempo getPlayTempo() {
        return playTempo;
    }

    public PressingIntensity getPressing() {
        return pressing;
    }

    public DefensiveLine getDefensiveLine() {
        return defensiveLine;
    }

    public PassingStyle getPassingStyle() {
        return passingStyle;
    }

    public AttackDirection getAttackDirection() {
        return attackDirection;
    }
}
