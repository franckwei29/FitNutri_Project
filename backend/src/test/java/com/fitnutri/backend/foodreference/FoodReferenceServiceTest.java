package com.fitnutri.backend.foodreference;

import com.fitnutri.backend.common.BusinessException;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class FoodReferenceServiceTest {
    private final FoodReferenceRepository foods=mock(FoodReferenceRepository.class);
    private final FoodReferenceService service=new FoodReferenceService(foods);
    @Test void invalidPagingAndOversizedQueryFailBeforeDatabaseRead() {
        assertThrows(BusinessException.class,()->service.searchActive("rice",-1,20));assertThrows(BusinessException.class,()->service.searchActive("rice",0,101));assertThrows(BusinessException.class,()->service.searchActive("x".repeat(201),0,20));verifyNoInteractions(foods);
    }
    @Test void unavailableCatalogueIsNotReportedAsAValidEmptySearch() {assertEquals("CATALOGUE_UNAVAILABLE",assertThrows(BusinessException.class,()->service.searchActive("rice",0,20)).code());verify(foods,never()).findAll(any(Specification.class),any(Pageable.class));}
    @Test void blankQueryKeepsPageShapeWithoutLoadingEveryReference() {when(foods.countSelectable()).thenReturn(1L);var result=service.searchActive(" ",2,10);assertEquals(List.of(),result.results());assertEquals(0,result.totalMatches());assertEquals(2,result.page());assertEquals(10,result.size());assertFalse(result.hasMore());verify(foods,never()).findAll();}
    private FoodReference reference() {var d=new FoodDataset();d.setId(3L);d.setName("SYNTHETIC");d.setRelease("test");d.setRevision("one");var f=new FoodReference();f.setId(9007199254740993L);f.setDataset(d);f.setPublicFoodKey("SYNTHETIC");f.setDisplayName("Synthetic");f.setProteinG100g(BigDecimal.ZERO);return f;}
    @Test void immutableInactiveReferenceRetainsStringIdentityUnknownNutrientsAndRealZero() {var f=reference();when(foods.findById(f.getId())).thenReturn(Optional.of(f));var dto=service.getImmutableReference(f.getId());assertEquals("9007199254740993",dto.referenceId());assertFalse(dto.active());assertNull(dto.per100g().energyKj());assertEquals(BigDecimal.ZERO,dto.per100g().proteinGrams());assertFalse(dto.dataset().verified());}
    @Test void explicitPublicKeyLookupNeverTreatsStringFoodKeyAsNumericPrimaryKey() {var f=reference();when(foods.findActiveByPublicFoodKey("SYNTHETIC")).thenReturn(Optional.of(f));assertEquals("9007199254740993",service.getActiveByPublicKey("SYNTHETIC").referenceId());verify(foods,never()).findById(any());}
}
