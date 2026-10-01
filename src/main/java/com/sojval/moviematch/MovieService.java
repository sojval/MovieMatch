package com.sojval.moviematch;

import com.sojval.moviematch.Model.Movie;
import com.sojval.moviematch.Model.MovieData;
import com.sojval.moviematch.Model.MovieMatch;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.embedding.EmbeddingModel;
import tools.jackson.core.type.TypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static io.micrometer.core.instrument.config.NamingConvention.dot;

@Service
public class MovieService {

    private final EmbeddingModel embeddingModel;
    private final JsonMapper jsonMapper;
    private final List<Movie> moviesEmbedding = new ArrayList<>();
    public MovieService(EmbeddingModel embeddingModel, JsonMapper jsonMapper) {
        this.embeddingModel = embeddingModel;
        this.jsonMapper = jsonMapper;
    }
    @PostConstruct
    public void initializeMovies() throws IOException {
        ClassPathResource resource = new ClassPathResource("movies.json");
        InputStream inputStream = resource.getInputStream();

        List<MovieData> movieDataList = jsonMapper.readValue(
                inputStream,new TypeReference<>() {}
        );
        for(MovieData movieData:movieDataList){
            float[] embedding = embeddingModel.embed(
                    movieData.getDescription()
            );
            Movie  movie = new Movie(
                    movieData.getTitle(),
                    movieData.getDescription(),
                    embedding
            );
            moviesEmbedding.add(movie);
        }
        inputStream.close();
        System.out.println(moviesEmbedding.size()+" movies embedding");
        for(Movie movie:moviesEmbedding){
            System.out.println(movie.getTitle()+" "+movie.getDescription());
        }
    }
    
    public List<MovieMatch> search(String query){
        float[] embedding = embeddingModel.embed(query);
        List<MovieMatch> matches = new ArrayList<>();
        for(Movie movie:moviesEmbedding){
            double similarity = cosineSimilarity(embedding,movie.getEmbedding());
            MovieMatch movieMatch = new MovieMatch(movie.getTitle(),movie.getDescription(),similarity);
            matches.add(movieMatch);
        }
        sortBySimilarity(matches);
        
        return topKMatches(matches,3);
    }
    public List<MovieMatch> similarMovies(String title){
        Movie selectedMovie = findMovie(title);
        List<MovieMatch> matches = new ArrayList<>();
        for(Movie movie:moviesEmbedding){
            if(movie.getTitle().equalsIgnoreCase(title)){
                continue;
            }
            
            double similarity = cosineSimilarity(selectedMovie.getEmbedding(),movie.getEmbedding());
            MovieMatch match = new MovieMatch(movie.getTitle(),movie.getDescription(),similarity);
            matches.add(match);
        }
        sortBySimilarity(matches);
        return topKMatches(matches,3);
    }

    private Movie findMovie(String title) {
        for (Movie movie : moviesEmbedding) {
            if (movie.getTitle().equalsIgnoreCase(title)) {
                return movie;
            }
        }

        throw new IllegalArgumentException(
                "Movie not found: " + title
        );
    }

    public double cosineSimilarity(float[] embedding1, float[] embedding2) {
        double dotProduct = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < embedding1.length; i++) {
            dotProduct +=(embedding1[i]*embedding2[i]);
            normA +=(embedding1[i]*embedding2[i]);
            normB +=(embedding1[i]*embedding2[i]);
        }
        if(normA==0 || normB==0){
            return 0;
        }
        return dotProduct/(Math.sqrt(normA)*Math.sqrt(normB));
    }
    private void sortBySimilarity(List<MovieMatch> matches) {
        Collections.sort(matches,
                (first,second)->Double.compare(
                        second.getMatch(),
                        first.getMatch()
                ));
    }
    
    private List<MovieMatch> topKMatches(List<MovieMatch> matches,int limit){
        List<MovieMatch> topMatches = new ArrayList<>();
        
        int numberOfMatches = Math.min(limit,matches.size());
        for(int i = 0;i<numberOfMatches;i++){
            topMatches.add(matches.get(i));
        }
        return topMatches;
    }

}
