package com.usg.book.adapter.in.web;

import com.usg.book.adapter.in.web.dto.BookRegisterRequest;
import com.usg.book.adapter.in.web.dto.BookRegisterResponse;
import com.usg.book.adapter.in.web.dto.GetBookResponse;
import com.usg.book.adapter.in.web.dto.BookUpdateRequest;
import com.usg.book.adapter.in.web.dto.BookUpdateResponse;
import com.usg.book.adapter.in.web.dto.Result;
import com.usg.book.adapter.in.web.token.MemberEmailGetter;
import com.usg.book.adapter.out.api.dto.BookAllResponse;
import com.usg.book.adapter.out.persistence.entity.BookEntity;
import com.usg.book.application.port.in.*;
import com.usg.book.application.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BookApiController {

    private final BookRegisterUseCase bookRegisterUseCase;
    private final BookImageUploadUseCase bookImageUploadUseCase;
    private final BookDeleteUseCase bookDeleteUseCase;
    private final BookUpdateUseCase bookUpdateUseCase;
    private final MemberEmailGetter memberEmailGetter;
    private final GetBookUseCase getBookUseCase;
    private final BookService bookService;

    @Operation(summary = "책 등록 *")
    @PostMapping(value = "/api/book", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Result> registerBook(@ModelAttribute("BookRegisterRequest") BookRegisterRequest request,
                                               HttpServletRequest servletRequest) {

        // JWT 에서 이메일 가져오기
        String email = memberEmailGetter.getMemberEmail(servletRequest.getHeader("Authorization"));
        BookRegisterCommend bookRegisterCommend = requestToCommend(request, email);
        Long savedBookId = bookRegisterUseCase.registerBook(bookRegisterCommend);

        // 책 저장과 이미지 저장 트랜잭션 분리
        bookImageUploadUseCase.saveImages(request.getImages(), savedBookId);

        return ResponseEntity.ok(new Result(BookRegisterResponse
                .builder()
                .savedBookId(savedBookId)
                .build(),
                "책 등록이 완료되었습니다."));
    }
    @GetMapping(value="/api/book")
    public ResponseEntity<Page<BookAllResponse>> findAll(@PageableDefault(page=1) Pageable pageable, HttpServletRequest servletRequest){
        Page<BookAllResponse> books=bookService.findAll(pageable);
        return ResponseEntity.ok(books);
    }

    @Operation(summary = "책 삭제 *")
    @DeleteMapping("/api/book/{bookId}")
    public ResponseEntity<Result> deleteBook(@PathVariable Long bookId,
                                             HttpServletRequest servletRequest) {

    // JWT 에서 이메일 가져오기
        //String email = memberEmailGetter.getMemberEmail(servletRequest.getHeader("Authorization"));
        BookDeleteCommend bookDeleteCommend = BookDeleteCommend.builder()
                //.email(email)
                .bookId(bookId)
                .build();

        bookDeleteUseCase.deleteBook(bookDeleteCommend);

        return ResponseEntity.ok(new Result(null, "책 삭제가 완료되었습니다."));
    }

    @Operation(summary = "책 수정 *")
    @PutMapping(value = "/api/book/{bookId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Result> updateBook(@PathVariable Long bookId,
                                         @ModelAttribute("BookUpdateRequest") BookUpdateRequest request,
                                         HttpServletRequest servletRequest) {

        // JWT 에서 이메일 가져오기
        //String email = memberEmailGetter.getMemberEmail(servletRequest.getHeader("Authorization"));
        BookUpdateCommend bookUpdateCommend = requestToUpdateCommend(request, "email", bookId);
        bookUpdateUseCase.updateBook(bookUpdateCommend);

        // 이미지 수정 로직 구현 (bookImageUpdateUseCase 사용)
        //bookImageUpdateUseCase.

        return ResponseEntity.ok(new Result(BookUpdateResponse.builder().updatedBookId(bookId).build(),"책 수정이 완료되었습니다."));
    }

    private BookRegisterCommend requestToCommend(BookRegisterRequest request, String email) {
        return BookRegisterCommend
                .builder()
                .email(email)
                .bookName(request.getBookName())
                .bookRealPrice(request.getBookRealPrice())
                .author(request.getAuthor())
                .publisher(request.getPublisher())
                .bookPostName(request.getBookPostName())
                .bookComment(request.getBookComment())
                .bookPrice(request.getBookPrice())
                .isbn(request.getIsbn())
                .build();
    }

    @Operation(summary = "책 상세 조회")
    @GetMapping("/api/book/{bookId}")
    public ResponseEntity<Result> getBook(@PathVariable(name = "bookId") Long bookId) {

        GetBookServiceResponse getBookServiceResponse = getBookUseCase.getBook(bookId);

        return ResponseEntity.ok(new Result(GetBookResponse
                .builder()
                .bookName(getBookServiceResponse.getBookName())
                .bookComment(getBookServiceResponse.getBookComment())
                .bookPostName(getBookServiceResponse.getBookPostName())
                .bookPrice(getBookServiceResponse.getBookPrice())
                .bookRealPrice(getBookServiceResponse.getBookRealPrice())
                .nickname(getBookServiceResponse.getNickname())
                .imageUrl(getBookServiceResponse.getImageUrl())
                .author(getBookServiceResponse.getAuthor())
                .publisher(getBookServiceResponse.getPublisher())
                .build(),
                "책 조회가 완료되었습니다."));
    }


    private BookUpdateCommend requestToUpdateCommend(BookUpdateRequest request, String email, Long bookId) {
        return BookUpdateCommend
                .builder()
                .email(email)
                .bookId(bookId)
                .bookPostName(request.getBookPostName())
                .bookComment(request.getBookComment())
                .bookPrice(request.getBookPrice())
                //.images(request.getImages())  // 이미지 수정 로직에 따라 수정
                .build();
    }
}
