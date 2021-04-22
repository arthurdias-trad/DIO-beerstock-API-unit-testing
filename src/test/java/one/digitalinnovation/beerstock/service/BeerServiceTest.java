package one.digitalinnovation.beerstock.service;

import one.digitalinnovation.beerstock.builder.BeerDTOBuilder;
import one.digitalinnovation.beerstock.dto.BeerDTO;
import one.digitalinnovation.beerstock.entity.Beer;
import one.digitalinnovation.beerstock.exception.BeerAlreadyRegisteredException;
import one.digitalinnovation.beerstock.exception.BeerNotFoundException;
import one.digitalinnovation.beerstock.exception.BeerStockExceededException;
import one.digitalinnovation.beerstock.mapper.BeerMapper;
import one.digitalinnovation.beerstock.repository.BeerRepository;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BeerServiceTest {

    private static final long INVALID_BEER_ID = 1L;

    @Mock
    private BeerRepository beerRepository;

    private BeerMapper beerMapper = BeerMapper.INSTANCE;

    @InjectMocks
    private BeerService beerService;

    @Test
    void whenBeerInformedThenItShouldBeCreated() throws BeerAlreadyRegisteredException {
        // given
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedSavedBeer = beerMapper.toModel(expectedBeerDTO);

        // when
        when(beerRepository.findByName(expectedBeerDTO.getName())).thenReturn(Optional.empty());
        when(beerRepository.save(expectedSavedBeer)).thenReturn(expectedSavedBeer);

        // then
        BeerDTO createdBeerDTO = beerService.createBeer(expectedBeerDTO);

        assertThat(createdBeerDTO.getId(), is(equalTo(expectedBeerDTO.getId())));
        assertThat(createdBeerDTO.getName(), is(equalTo(expectedBeerDTO.getName())));
        assertThat(createdBeerDTO.getQuantity(), is(equalTo(expectedBeerDTO.getQuantity())));

        assertThat(createdBeerDTO.getQuantity(), is(greaterThan(2)));
    }

    @Test
    void whenInformedBeerIsAlreadyRegisteredThenExceptionShouldBeThrown() {
        //given
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer duplicatedBeer = beerMapper.toModel(expectedBeerDTO);

        // when
        when(beerRepository.findByName(expectedBeerDTO.getName())).thenReturn(Optional.of(duplicatedBeer));

        // then
        assertThrows(BeerAlreadyRegisteredException.class, () -> beerService.createBeer(expectedBeerDTO));
    }

    @Test
    void whenValidBeerNameIsGivenThenBeerShouldBeReturned() throws BeerNotFoundException {
        // given
        BeerDTO expectedFoundBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedFoundBeer = beerMapper.toModel(expectedFoundBeerDTO);

        // when
        when(beerRepository.findByName(expectedFoundBeer.getName())).thenReturn(Optional.of(expectedFoundBeer));

        //then
        BeerDTO foundBeerDTO = beerService.findByName(expectedFoundBeer.getName());

        assertThat(foundBeerDTO, is(equalTo(expectedFoundBeerDTO)));
    }

    @Test
    void whenInvalidBeerNameIsGivenThenExceptionShouldBeThrown() {
        // given
        BeerDTO expectedFoundBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        // when
        when(beerRepository.findByName(expectedFoundBeerDTO.getName())).thenReturn(Optional.empty());

        // then
        assertThrows(BeerNotFoundException.class, () -> beerService.findByName(expectedFoundBeerDTO.getName()));
    }

    @Test
    void whenListBeerIsCalledThenReturnListOfAllBeers() {
        // given
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer beer = beerMapper.toModel(beerDTO);
        List<Beer> expectedBeerList = Collections.singletonList(beer);

        // when
        when(beerRepository.findAll()).thenReturn(expectedBeerList);

        //then
        List<BeerDTO> foundBeerList = beerService.listAll();

        assertThat(foundBeerList, is(not(empty())));
        assertThat(foundBeerList.get(0), is(equalTo(beerDTO)));
    }

    @Test
    void whenListBeerIsCalledThenReturnAnEmptyList() {
        // when
        when(beerRepository.findAll()).thenReturn(Collections.emptyList());

        // then
        List<BeerDTO> foundBeerList = beerService.listAll();

        assertThat(foundBeerList, is(empty()));
    }

    @Test
    void whenDeleteByIdIsCalledWithValidIdThenBeerShouldBeDeleted() throws BeerNotFoundException {
        // given
        BeerDTO expectedDeletedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedDeletedBeer = beerMapper.toModel(expectedDeletedBeerDTO);

        // when
        when(beerRepository.findById(expectedDeletedBeerDTO.getId())).thenReturn(Optional.of(expectedDeletedBeer));
        doNothing().when(beerRepository).deleteById(expectedDeletedBeerDTO.getId());

        // then
        beerService.deleteById(expectedDeletedBeerDTO.getId());

        verify(beerRepository, times(1)).findById(expectedDeletedBeerDTO.getId());
        verify(beerRepository, times(1)).deleteById(expectedDeletedBeerDTO.getId());
    }

    @Test
    void whenDeleteByIdIsCalledWIthInvalidIdThenExceptionShouldBeThrown () throws BeerNotFoundException {
        // when
        when(beerRepository.findById(INVALID_BEER_ID)).thenReturn(Optional.empty());

        // then
        assertThrows(BeerNotFoundException.class, () -> beerService.deleteById(INVALID_BEER_ID));

    }

    @Test
    void whenIncrementIsCalledThenIncrementBeerStock() throws BeerNotFoundException, BeerStockExceededException {
        // given
        BeerDTO beerToIncrementDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer beerToIncrement = beerMapper.toModel(beerToIncrementDTO);

        // when
        when(beerRepository.findById(beerToIncrementDTO.getId())).thenReturn(Optional.of(beerToIncrement));
        when(beerRepository.save(beerToIncrement)).thenReturn(beerToIncrement);

        // then
        int quantityToIncrement = 10;
        int expectedQuantityAfterIncrement = beerToIncrementDTO.getQuantity() + quantityToIncrement;
        BeerDTO incrementedBeerDTO = beerService.increment(beerToIncrementDTO.getId(), quantityToIncrement);

        assertThat(expectedQuantityAfterIncrement, is(equalTo(incrementedBeerDTO.getQuantity())));
        assertThat(incrementedBeerDTO.getQuantity(), is(lessThan(beerToIncrementDTO.getMax())));
    }

    @Test
    void whenIncrementIsGreaterThanMaxThenThrowException() {
        // given
        BeerDTO beerToIncrementDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer beerToIncrement = beerMapper.toModel(beerToIncrementDTO);

        // when
        when(beerRepository.findById(beerToIncrementDTO.getId())).thenReturn(Optional.of(beerToIncrement));

        // then
        int quantityToIncrement = beerToIncrementDTO.getQuantity() + beerToIncrement.getMax();

        assertThat(quantityToIncrement, is(greaterThan(beerToIncrementDTO.getMax())));
        assertThrows(BeerStockExceededException.class, () -> beerService.increment(beerToIncrementDTO.getId(), quantityToIncrement));
    }

    @Test
    void whenIncrementIsCalledWithInvalidIdThenThrowException() {
        // given
        BeerDTO beerToIncrementDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        // when
        when(beerRepository.findById(beerToIncrementDTO.getId())).thenReturn(Optional.empty());

        // then
        int quantityToIncrement = 10;
        assertThrows(BeerNotFoundException.class, () -> beerService.increment(beerToIncrementDTO.getId(), quantityToIncrement));
    }

    @Test
    void whenDecrementIsCalledThenDecrementBeerStock() throws BeerNotFoundException, BeerStockExceededException {
        // given
        BeerDTO beerToDecrementDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer beerToDecrement = beerMapper.toModel(beerToDecrementDTO);

        // when
        when(beerRepository.findById(beerToDecrementDTO.getId())).thenReturn(Optional.of(beerToDecrement));
        when(beerRepository.save(beerToDecrement)).thenReturn(beerToDecrement);

        // then
        int quantityToDecrement = 10;
        int expectedQuantityAfterDecrement = beerToDecrementDTO.getQuantity() - Math.abs(quantityToDecrement);
        BeerDTO decrementedBeerDTO = beerService.decrement(beerToDecrementDTO.getId(), quantityToDecrement);

        assertThat(expectedQuantityAfterDecrement, is(equalTo(decrementedBeerDTO.getQuantity())));
        assertThat(decrementedBeerDTO.getQuantity(), is(lessThan(beerToDecrementDTO.getMax())));
    }

    @Test
    void whenDecrementIsCalledWithInvalidIdThenThrowException() {
        // when
        when(beerRepository.findById(INVALID_BEER_ID)).thenReturn(Optional.empty());

        // then
        int quantityToDecrement = 10;
        assertThrows(BeerNotFoundException.class, () -> beerService.decrement(INVALID_BEER_ID, quantityToDecrement));
    }

    @Test
    void whenDecrementIsCalledAndDecrementIsGreaterThanStockThenThrowException() {
        // given
        BeerDTO beerToDecrementDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer beerToDecrement = beerMapper.toModel(beerToDecrementDTO);

        // when
        when(beerRepository.findById(beerToDecrementDTO.getId())).thenReturn(Optional.of(beerToDecrement));

        // then
        int quantityToDecrement = beerToDecrement.getQuantity() * 2;
        int expectedQuantityAfterDecrement = beerToDecrement.getQuantity() - quantityToDecrement;
        assertThat(expectedQuantityAfterDecrement, is(lessThan(0)));
        assertThrows(BeerStockExceededException.class, () -> beerService.decrement(beerToDecrement.getId(), quantityToDecrement));
    }
}
