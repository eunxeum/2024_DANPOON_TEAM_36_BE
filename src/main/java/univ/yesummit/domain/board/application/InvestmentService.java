package univ.yesummit.domain.board.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import univ.yesummit.domain.board.api.dto.response.InvestmentReceivedResDto;
import univ.yesummit.domain.board.api.dto.response.InvestmentResDto;
import univ.yesummit.domain.board.domain.Board;
import univ.yesummit.domain.board.domain.Investment;
import univ.yesummit.domain.board.domain.repository.BoardRepository;
import univ.yesummit.domain.board.domain.repository.InvestmentRepository;
import univ.yesummit.domain.member.entity.Member;
import univ.yesummit.domain.member.repository.MemberRepository;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvestmentService {

    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final InvestmentRepository investmentRepository;

    @Transactional
    public void addInvestment(Long memberId, Long boardId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        if (investmentRepository.existsByBoardAndMember(board, member)) {
            throw new IllegalStateException("이미 투자 제안 중입니다.");
        }

        board.updateInvestmentCount();
        investmentRepository.save(Investment.builder()
                .board(board)
                .member(member)
                .build());
    }

    @Transactional
    public void cancelInvestment(Long memberId, Long boardId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        Investment investment = investmentRepository.findByBoardAndMember(board, member)
                .orElseThrow(() -> new IllegalArgumentException("투자 기록이 존재하지 않습니다."));

        board.cancelInvestmentCount();
        investmentRepository.delete(investment);
    }

    public List<InvestmentResDto> getInvestedBoardsWithDetailsByMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        List<Investment> investments = investmentRepository.findByMember(member);
        return investments.stream()
                .map(investment -> {
                    Board board = investment.getBoard();
                    return InvestmentResDto.of(
                            member.getId(),
                            member.getUsername(),
                            member.getPosition(),
                            member.getPhoneNumber(),
                            member.getEmail(),
                            board.getBoardId(),
                            board.getTitle(),
                            board.getContent(),
                            board.getInvestmentCount()
                    );
                })
                .collect(Collectors.toList());
    }

    // 내가 투자받은 투자자 목록 조회
    public List<InvestmentReceivedResDto> getReceivedInvestments(Long memberId) {
        // 작성한 게시글에 대한 투자 기록 조회
        List<Investment> investments = investmentRepository.findAllByBoardWriterId(memberId);

        // 투자 정보를 DTO로 변환
        return investments.stream()
                .map(investment -> {
                    Member investor = investment.getMember(); // 투자자 정보
                    Board board = investment.getBoard(); // 게시글 정보

                    return InvestmentReceivedResDto.builder()
                            .boardId(board.getBoardId())
                            .boardTitle(board.getTitle())
                            .investorId(investor.getId())
                            .investorName(investor.getUsername())
                            .investorEmail(investor.getEmail())
                            .investorPhoneNumber(investor.getPhoneNumber())
                            .investorPosition(investor.getPosition())
                            .build();
                })
                .toList();
    }
}