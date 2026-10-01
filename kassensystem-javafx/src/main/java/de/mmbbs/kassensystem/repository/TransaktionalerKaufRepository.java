package de.mmbbs.kassensystem.repository;

import de.mmbbs.kassensystem.model.Bon;
import java.util.Map;

/** Speichert einen Kauf mit allen Bestandsaenderungen als Einheit. */
public interface TransaktionalerKaufRepository {
    void speichereKauf(Bon bon, Map<Integer, Double> mengen);
}
