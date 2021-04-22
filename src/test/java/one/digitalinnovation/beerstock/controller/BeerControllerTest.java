package one.digitalinnovation.beerstock.controller;

import one.digitalinnovation.beerstock.builder.BeerDTOBuilder;
import one.digitalinnovation.beerstock.dto.BeerDTO;
import one.digitalinnovation.beerstock.dto.QuantityDTO;
import one.digitalinnovation.beerstock.entity.Beer;
import one.digitalinnovation.beerstock.exception.BeerAlreadyRegisteredException;
import one.digitalinnovation.beerstock.exception.BeerNotFoundException;
import one.digitalinnovation.beerstock.exception.BeerStockExceededException;
import one.digitalinnovation.beerstock.service.BeerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import java.util.Collections;
import java.util.List;

import static one.digitalinnovation.beerstock.utils.JsonConvertionUtils.asJsonString;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.mock.http.server.reactive.MockServerHttpRequest.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class BeerControllerTest {

    private static final String BEER_API_URL_PATH = "/api/v1/beers";
    private static final long VALID_BEER_ID = 1L;
    private static final long INVALID_BEER_ID = 2l;
    private static final String BEER_API_SUBPATH_INCREMENT_URL = "/increment";
    private static final String BEER_API_SUBPATH_DECREMENT_URL = "/decrement";

    private MockMvc mockMvc;

    @Mock
    private BeerService beerService;

    @InjectMocks
    private BeerController beerController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(beerController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setViewResolvers((s, locale) -> new MappingJackson2JsonView())
                .build();
    }

    @Test
    void whenPOSTIsCalledThenABeerIsCreated() throws Exception {
        // given
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        // when
        when(beerService.createBeer(beerDTO)).thenReturn(beerDTO);

        // then
        mockMvc.perform(post(BEER_API_URL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(beerDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is(beerDTO.getName())))
                .andExpect(jsonPath("$.brand", is(beerDTO.getBrand())))
                .andExpect(jsonPath("$.type", is(beerDTO.getType().toString())));

    }

    @Test
    void whenPOSTIsCalledWithoutRequiredFieldThenAnErrorIsReturned() throws Exception {
        // given
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        beerDTO.setName(null);

        // then
        mockMvc.perform(post(BEER_API_URL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(beerDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenGETIsCalledWithAValidNameThenStatusOkAndABeerAreReturned() throws Exception {
        // given
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        // when
        when(beerService.findByName(beerDTO.getName())).thenReturn(beerDTO);

        //then
        mockMvc.perform(get(BEER_API_URL_PATH + "/" + beerDTO.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(beerDTO.getName())))
                .andExpect(jsonPath("$.brand", is(beerDTO.getBrand())))
                .andExpect(jsonPath("$.type", is(beerDTO.getType().toString())));

    }

    @Test
    void whenGETIsCalledWithAnInvalidNameThenStatusNotFoundIsReturned() throws Exception {
        // given
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        // when
        when(beerService.findByName(beerDTO.getName())).thenThrow(BeerNotFoundException.class);

        // then
        mockMvc.perform(get(BEER_API_URL_PATH + "/" + beerDTO.getName()))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenGETListIsCalledWithBeersThenReturnStatusOkAndListOfAllBeers() throws Exception {
        // given
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        List<BeerDTO> expectedBeerList = List.of(beerDTO);

        // when
        when(beerService.listAll()).thenReturn(expectedBeerList);

        // then
        mockMvc.perform(get(BEER_API_URL_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is(expectedBeerList.get(0).getName())))
                .andExpect(jsonPath("$[0].brand", is(expectedBeerList.get(0).getBrand())))
                .andExpect(jsonPath("$[0].type", is(expectedBeerList.get(0).getType().toString())));
    }

    @Test
    void whenGETListIsCalledWithoutBeersThenReturnStatusOkAndEmptyList() throws Exception {
        // when
        when(beerService.listAll()).thenReturn(Collections.emptyList());

        // then
        mockMvc.perform(get(BEER_API_URL_PATH))
                .andExpect(status().isOk());
    }

    @Test
    void whenDELETEIsCalledWithValidIdThenReturnStatusNoContent() throws Exception {
        // when
        doNothing().when(beerService).deleteById(VALID_BEER_ID);

        //then
        mockMvc.perform(delete(BEER_API_URL_PATH + "/" + VALID_BEER_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void whenDELETEIsCalledWithInvalidIdThenReturnStatusNotFound() throws Exception {
        // when
        doThrow(BeerNotFoundException.class).when(beerService).deleteById(INVALID_BEER_ID);

        //then
        mockMvc.perform(delete(BEER_API_URL_PATH + "/" + INVALID_BEER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenPATCHIsCalledWithValidIdAndIncrementThenReturnStatusOkAndBeerDto() throws Exception {
        // given
        QuantityDTO quantityDTO = QuantityDTO.builder()
                .quantity(10)
                .build();

        BeerDTO beerToIncrementDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        beerToIncrementDTO.setQuantity(quantityDTO.getQuantity() + beerToIncrementDTO.getQuantity());

        // when
        when(beerService.increment(beerToIncrementDTO.getId(), quantityDTO.getQuantity())).thenReturn(beerToIncrementDTO);

        // then
        mockMvc.perform(MockMvcRequestBuilders.patch(BEER_API_URL_PATH
                + "/" + VALID_BEER_ID
                + BEER_API_SUBPATH_INCREMENT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(quantityDTO))).andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(beerToIncrementDTO.getName())))
                .andExpect(jsonPath("$.quantity", is(beerToIncrementDTO.getQuantity())));

    }

    @Test
    void whenPATCHIsCalledWithInvalidIdThenReturnStatusNotFound() throws Exception {
        // given
        QuantityDTO quantityDTO = QuantityDTO.builder()
                .quantity(10)
                .build();

        // when
        when(beerService.increment(INVALID_BEER_ID, quantityDTO.getQuantity())).thenThrow(BeerNotFoundException.class);

        // then
        mockMvc.perform(MockMvcRequestBuilders.patch(BEER_API_URL_PATH
                + "/" + INVALID_BEER_ID
                + BEER_API_SUBPATH_INCREMENT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(quantityDTO))).andExpect(status().isNotFound());
    }

    @Test
    void whenPATCHIsCalledWithIncrementGreaterThanMaxThenReturnStatusBadRequest() throws Exception {
        // given
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        QuantityDTO quantityDTO = QuantityDTO.builder()
                .quantity(beerDTO.getMax() + 1)
                .build();

        // when
        when(beerService.increment(beerDTO.getId(), quantityDTO.getQuantity())).thenThrow(BeerStockExceededException.class);

        // then
        mockMvc.perform(MockMvcRequestBuilders.patch(BEER_API_URL_PATH
                + "/" + beerDTO.getId()
                + BEER_API_SUBPATH_INCREMENT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(quantityDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenPATCHIsCalledWithValidIdAndDecrementThenReturnStatusOkAndBeerDTO() throws Exception {
        // given
        QuantityDTO quantityDTO = QuantityDTO.builder()
                .quantity(10)
                .build();

        BeerDTO beerToDecrementDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        beerToDecrementDTO.setQuantity(quantityDTO.getQuantity() - beerToDecrementDTO.getQuantity());

        // when
        when(beerService.decrement(beerToDecrementDTO.getId(), quantityDTO.getQuantity())).thenReturn(beerToDecrementDTO);

        // then
        mockMvc.perform(MockMvcRequestBuilders.patch(BEER_API_URL_PATH
                + "/" + VALID_BEER_ID
                + BEER_API_SUBPATH_DECREMENT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(quantityDTO))).andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(beerToDecrementDTO.getName())))
                .andExpect(jsonPath("$.quantity", is(beerToDecrementDTO.getQuantity())));
    }

    @Test
    void whenPATCHIsCalledWithInvalidIdToDecrementThenReturnStatusNotFound() throws Exception {
        // given
        QuantityDTO quantityDTO = QuantityDTO.builder()
                .quantity(10)
                .build();

        // when
        when(beerService.decrement(INVALID_BEER_ID, quantityDTO.getQuantity())).thenThrow(BeerNotFoundException.class);

        // then
        mockMvc.perform(MockMvcRequestBuilders.patch(BEER_API_URL_PATH
                + "/" + INVALID_BEER_ID
                + BEER_API_SUBPATH_DECREMENT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(quantityDTO))).andExpect(status().isNotFound());
    }

    @Test
    void whenPATCHIsCalledWithDecrementGreaterThanStockThenReturnStatusBadRequest() throws Exception {
        // given
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        QuantityDTO quantityDTO = QuantityDTO.builder()
                .quantity(beerDTO.getQuantity() * 2)
                .build();

        // when
        when(beerService.decrement(beerDTO.getId(), quantityDTO.getQuantity())).thenThrow(BeerStockExceededException.class);

        // then
        mockMvc.perform(MockMvcRequestBuilders.patch(BEER_API_URL_PATH
                + "/" + beerDTO.getId()
                + BEER_API_SUBPATH_DECREMENT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(quantityDTO)))
                .andExpect(status().isBadRequest());
    }
}
