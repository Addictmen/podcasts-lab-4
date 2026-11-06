
package com.example.podcasts.service;

import com.example.podcasts.model.Episode;
import com.example.podcasts.model.Podcast;
import com.example.podcasts.model.PodcastStatus;
import com.example.podcasts.repository.EpisodeRepository;
import com.example.podcasts.repository.PodcastRepository;
import com.example.podcasts.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PodcastService {

    @Autowired
    private PodcastRepository podcastRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private EpisodeService episodeService;

    @PreAuthorize("hasRole('ADMIN')")
    public List<Podcast> getAll() {
        return podcastRepository.findAll();
    }

    @PreAuthorize("@authz.ownsPodcast(authentication, #id)")
    public Podcast getById(Long id) {
        return podcastRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Подкаст не найден"
                ));
    }

    @Transactional
    @PreAuthorize("@authz.canCreatePodcast(authentication, #data.authorId)")
    public Podcast create(Podcast data) {
        if (data.getTitle() == null ||
                data.getTitle().isBlank() ||
                data.getAuthorId() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Нужно указать title и authorId"
            );
        }

        if (!userRepository.existsById(data.getAuthorId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Автор не найден"
            );
        }

        Podcast podcast = new Podcast();
        podcast.setTitle(data.getTitle());
        podcast.setAuthorId(data.getAuthorId());

        if (data.getStatus() != null) {
            podcast.setStatus(data.getStatus());
        } else {
            podcast.setStatus(PodcastStatus.DRAFT);
        }

        return podcastRepository.save(podcast);
    }

    @Transactional
    @PreAuthorize("@authz.ownsPodcast(authentication, #id) and (#data.authorId == null or @authz.canCreatePodcast(authentication, #data.authorId))")
    public Podcast update(Long id, Podcast data) {
        Podcast podcast = getById(id);

        if (data.getTitle() != null &&
                data.getTitle().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Название не может быть пустым"
            );
        }

        if (data.getAuthorId() != null &&
                !userRepository.existsById(data.getAuthorId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Автор не найден"
            );
        }

        if (data.getTitle() != null) {
            podcast.setTitle(data.getTitle());
        }

        if (data.getStatus() != null) {
            podcast.setStatus(data.getStatus());
        }

        if (data.getAuthorId() != null) {
            podcast.setAuthorId(data.getAuthorId());
        }

        return podcastRepository.save(podcast);
    }

    @Transactional
    @PreAuthorize("@authz.ownsPodcast(authentication, #id)")
    public void delete(Long id) {
        getById(id);

        // Удаляем связанные эпизоды
        for (Episode episode : episodeRepository.findByPodcastId(id)) {
            episodeService.delete(episode.getId());
        }

        // Удаляем сам подкаст
        podcastRepository.deleteById(id);
    }
}
