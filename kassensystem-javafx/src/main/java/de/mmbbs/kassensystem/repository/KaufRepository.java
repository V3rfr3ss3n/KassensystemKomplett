package de.mmbbs.kassensystem.repository;

import de.mmbbs.kassensystem.model.Bon;
import java.util.List;
import java.util.Map;

/** Der Server speichert Bon und Bestandsänderung gemeinsam. */
public interface KaufRepository {
    List<Bon> ladeBonHistorie();
    Bon schliesseKaufAb(Map<Integer, Double> mengen);
}
