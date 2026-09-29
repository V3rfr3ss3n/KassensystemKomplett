package de.mmbbs.kassensystem.repository;

import de.mmbbs.kassensystem.model.Bon;
import java.util.Map;

public interface RemoteKaufRepository {
    Bon schliesseKaufAb(Map<Integer, Double> mengen);
}
