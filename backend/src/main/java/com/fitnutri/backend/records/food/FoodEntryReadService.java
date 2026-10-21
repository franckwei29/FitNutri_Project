package com.fitnutri.backend.records.food;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.records.MealType;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class FoodEntryReadService {
 private final FoodEntryReadRepository roots;private final FoodSnapshotReader snapshots;private final RecordDatePolicy dates;
 public FoodEntryReadService(FoodEntryReadRepository roots,FoodSnapshotReader snapshots,RecordDatePolicy dates){this.roots=roots;this.snapshots=snapshots;this.dates=dates;}
 public FoodSnapshotReader.Page listRoots(Long owner,LocalDate from,LocalDate to,MealType meal,String zone,int page,int size){dates.requireRange(from,to,zone);if(page<0||size<1||size>100)throw BusinessException.invalid("page","Choose a nonnegative page and a size from 1 to 100.");var keys=roots.listRoots(owner,from,to,meal,page,size);var rows=snapshots.batchOwnedRoots(owner,keys.values());return new FoodSnapshotReader.Page(rows,keys.total(),page,size,(long)page*size+rows.size()<keys.total());}
 public FoodSnapshotReader.Root getOwnedRoot(Long owner,String kind,long id){return snapshots.detailForReuse(owner,kind,id);}
}
