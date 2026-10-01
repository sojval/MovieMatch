package com.sojval.moviematch.Model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Movie {
    private String title;
    private String description;
    private float[] embedding;
}
