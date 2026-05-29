package de.mmbbs.kassensystem.repository;

import de.mmbbs.kassensystem.model.Bon;

import java.util.List;

public interface BonHistorieRepository {
    List<Bon> ladeBonHistorie();

    void speichereBonHistorie(List<Bon> bonHistorie);
}
