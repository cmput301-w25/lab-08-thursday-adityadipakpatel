package com.example.androidcicd.movie;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MovieProvider {
    private static MovieProvider movieProvider;
    private final ArrayList<Movie> movies;
    private final CollectionReference movieCollection;

    private MovieProvider(FirebaseFirestore firestore) {
        movies = new ArrayList<>();
        movieCollection = firestore.collection("movies");
    }

    public interface DataStatus {
        void onDataUpdated();
        void onError(String error);
    }

    public void listenForUpdates(final DataStatus dataStatus) {
        movieCollection.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                dataStatus.onError(error.getMessage());
                return;
            }
            movies.clear();
            if (snapshot != null) {
                for (QueryDocumentSnapshot item : snapshot) {
                    movies.add(item.toObject(Movie.class));
                }
                dataStatus.onDataUpdated();
            }
        });
    }

    public static MovieProvider getInstance(FirebaseFirestore firestore) {
        if (movieProvider == null)
            movieProvider = new MovieProvider(firestore);
        return movieProvider;
    }

    public ArrayList<Movie> getMovies() {
        return movies;
    }

    public void updateMovie(Movie movie, String title, String genre, int year) {
        // We only consider it a “new” title if the user is changing it to something else
        // But for simplicity, we’ll just enforce uniqueness every time, skipping the same movie’s ID
        if (!isTitleUnique(title, movie.getId())) {
            throw new IllegalArgumentException("Duplicate Movie Title!");
        }

        movie.setTitle(title);
        movie.setGenre(genre);
        movie.setYear(year);

        DocumentReference docRef = movieCollection.document(movie.getId());
        if (validMovie(movie, docRef)) {
            docRef.set(movie);
        } else {
            throw new IllegalArgumentException("Invalid Movie!");
        }
    }

    public void addMovie(Movie movie) {
        DocumentReference docRef = movieCollection.document();
        movie.setId(docRef.getId());

        // Enforce uniqueness
        if (!isTitleUnique(movie.getTitle(), null)) {
            // We throw an exception or handle this differently
            throw new IllegalArgumentException("Duplicate Movie Title!");
        }

        if (validMovie(movie, docRef)) {
            docRef.set(movie);
        } else {
            throw new IllegalArgumentException("Invalid Movie!");
        }
    }

    public void deleteMovie(Movie movie) {
        DocumentReference docRef = movieCollection.document(movie.getId());
        docRef.delete();
    }

    public boolean validMovie(Movie movie, DocumentReference docRef) {
        return movie.getId().equals(docRef.getId()) && !movie.getTitle().isEmpty() && !movie.getGenre().isEmpty() && movie.getYear() > 0;
    }

    private boolean isTitleUnique(String title, @Nullable String ignoreMovieId) {
        for (Movie m : movies) {
            // If we are editing a movie, we should skip checking its own ID
            if (ignoreMovieId != null && m.getId().equals(ignoreMovieId)) {
                continue;
            }
            // Compare titles case-insensitively, or adjust as you prefer
            if (m.getTitle().equalsIgnoreCase(title)) {
                return false;
            }
        }
        return true;
    }

}
