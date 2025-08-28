package br.com.lumilivre.api.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.data.AlunoDTO;
import br.com.lumilivre.api.data.ListaAlunoDTO;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.CursoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.model.UsuarioModel;
import br.com.lumilivre.api.repository.AlunoRepository;
import br.com.lumilivre.api.repository.CursoRepository;
import br.com.lumilivre.api.repository.UsuarioRepository;
import br.com.lumilivre.api.utils.CpfValidator;


@Service
public class AlunoService {

    @Autowired
    private AlunoRepository ar;

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private UsuarioRepository ur;

    @Autowired
    private EmailService emailService; 

    private final PasswordEncoder passwordEncoder;

    @Autowired
    private ResponseModel rm;

    @Autowired
    private CepService cepService;

    AlunoService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public Page<ListaAlunoDTO> buscarAlunosParaListaAdmin(Pageable pageable) {
        return ar.findAlunosParaListaAdmin(pageable);
    }

    public Page<AlunoModel> buscarPorTexto(String texto, Pageable pageable) {
        if (texto == null || texto.isBlank()) {
            return ar.findAll(pageable);
        }
        return ar.buscarPorTexto(texto, pageable);
    }

    public Page<AlunoModel> buscarAvancado(String nome, String matricula, LocalDate dataNascimento,
            String cursoNome, Pageable pageable) {
        return ar.buscarAvancado(nome, matricula, dataNascimento, cursoNome, pageable);
    }

    public ResponseEntity<?> cadastrar(AlunoDTO dto) {
        if (dto.getMatricula() == null || dto.getMatricula().trim().isEmpty()) {
            rm.setMensagem("A matrícula é obrigatória.");
            return ResponseEntity.badRequest().body(rm);
        }
        if (!dto.getMatricula().matches("\\d{5}")) {
            rm.setMensagem("A matrícula deve conter 5 dígitos numéricos.");
            return ResponseEntity.badRequest().body(rm);
        }
        if (ar.existsById(dto.getMatricula())) {
            rm.setMensagem("Essa matrícula já está cadastrada.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (dto.getNome() == null || dto.getNome().trim().isEmpty()) {
            rm.setMensagem("O nome é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }
        if (dto.getSobrenome() == null || dto.getSobrenome().trim().isEmpty()) {
            rm.setMensagem("O sobrenome é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }
        if (dto.getCpf() == null || dto.getCpf().trim().isEmpty()) {
            rm.setMensagem("O CPF é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }
        if (!CpfValidator.isCpfValido(dto.getCpf())) {
            rm.setMensagem("CPF inválido.");
            return ResponseEntity.badRequest().body(rm);
        }
        if (ar.existsById(dto.getCpf())) {
            rm.setMensagem("CPF já cadastrado.");
            return ResponseEntity.badRequest().body(rm);
        }
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            rm.setMensagem("O email é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }
        if (dto.getCelular() == null || dto.getCelular().trim().isEmpty()) {
            rm.setMensagem("O celular é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }
        if (dto.getCursoId() == null) {
            rm.setMensagem("O curso é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        Optional<CursoModel> curso = cursoRepository.findById(dto.getCursoId());
        if (curso.isEmpty()) {
            rm.setMensagem("Curso não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        AlunoDTO enderecoDTO = cepService.buscarEnderecoPorCep(dto.getCep());
        if (enderecoDTO == null || enderecoDTO.getCep() == null) {
            rm.setMensagem("CEP inválido ou não encontrado.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(rm);
        }

        AlunoModel aluno = new AlunoModel();
        aluno.setMatricula(dto.getMatricula());
        aluno.setNome(dto.getNome());
        aluno.setSobrenome(dto.getSobrenome());
        aluno.setCpf(dto.getCpf());
        aluno.setDataNascimento(dto.getDataNascimento());
        aluno.setCelular(dto.getCelular());
        aluno.setEmail(dto.getEmail());
        aluno.setCurso(curso.get());
        aluno.setCep(dto.getCep());
        aluno.setLogradouro(enderecoDTO.getLogradouro());
        aluno.setComplemento(enderecoDTO.getComplemento());
        aluno.setLocalidade(enderecoDTO.getLocalidade());
        aluno.setBairro(enderecoDTO.getBairro());
        aluno.setUf(enderecoDTO.getUf());
        aluno.setEstado(enderecoDTO.getEstado());
        aluno.setNumero_casa(dto.getNumero_casa());

        UsuarioModel usuario = new UsuarioModel();
        usuario.setEmail(dto.getMatricula());
        usuario.setSenha(passwordEncoder.encode(dto.getCpf()));
        usuario.setRole(Role.ALUNO);
        usuario.setAluno(aluno);
        aluno.setUsuario(usuario);

        AlunoModel salvo = ar.save(aluno);
        emailService.enviarSenhaInicial(aluno.getEmail(), aluno.getNome(), dto.getCpf());

        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    public ResponseEntity<?> atualizar(String matricula, AlunoDTO dto) {
        Optional<AlunoModel> alunoOpt = ar.findById(matricula);
        if (alunoOpt.isEmpty()) {
            rm.setMensagem("Aluno não encontrado para a matrícula: " + matricula);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(rm);
        }

        AlunoModel aluno = alunoOpt.get();

        if (dto.getNome() == null || dto.getNome().trim().isEmpty()) {
            rm.setMensagem("O nome é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }
        if (dto.getSobrenome() == null || dto.getSobrenome().trim().isEmpty()) {
            rm.setMensagem("O sobrenome é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }
        if (dto.getCpf() == null || dto.getCpf().trim().isEmpty()) {
            rm.setMensagem("O CPF é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }
        if (!CpfValidator.isCpfValido(dto.getCpf())) {
            rm.setMensagem("CPF inválido.");
            return ResponseEntity.badRequest().body(rm);
        }

        var curso = cursoRepository.findById(dto.getCursoId());
        if (curso.isEmpty()) {
            rm.setMensagem("Curso não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        AlunoDTO enderecoDTO = cepService.buscarEnderecoPorCep(dto.getCep());
        if (enderecoDTO == null || enderecoDTO.getCep() == null) {
            rm.setMensagem("CEP inválido ou não encontrado.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(rm);
        }

        boolean cpfAlterado = !aluno.getCpf().equals(dto.getCpf());

        aluno.setNome(dto.getNome());
        aluno.setSobrenome(dto.getSobrenome());
        aluno.setCpf(dto.getCpf());
        aluno.setDataNascimento(dto.getDataNascimento());
        aluno.setCelular(dto.getCelular());
        aluno.setEmail(dto.getEmail());
        aluno.setCurso(curso.get());
        aluno.setCep(dto.getCep());
        aluno.setLogradouro(enderecoDTO.getLogradouro());
        aluno.setComplemento(enderecoDTO.getComplemento());
        aluno.setLocalidade(enderecoDTO.getLocalidade());
        aluno.setBairro(enderecoDTO.getBairro());
        aluno.setUf(enderecoDTO.getUf());
        aluno.setEstado(enderecoDTO.getEstado());
        aluno.setNumero_casa(dto.getNumero_casa());

        if (cpfAlterado && aluno.getUsuario() != null) {
            aluno.getUsuario().setSenha(passwordEncoder.encode(dto.getCpf()));
            emailService.enviarSenhaInicial(aluno.getEmail(), aluno.getNome(), dto.getCpf());
        }

        AlunoModel salvo = ar.save(aluno);
        return ResponseEntity.ok(salvo);
    }


    public ResponseEntity<ResponseModel> excluir(String matricula) {
        var alunoOpt = ar.findById(matricula);

        if (alunoOpt.isEmpty()) {
            rm.setMensagem("Aluno não encontrado.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(rm);
        }

        AlunoModel aluno = alunoOpt.get();

        if (aluno.getUsuario() != null) {
            ur.delete(aluno.getUsuario());
        }

        ar.delete(aluno);

        rm.setMensagem("Aluno e usuário associados removidos com sucesso.");
        return ResponseEntity.ok(rm);
    }


    public Optional<AlunoModel> buscarPorNome(String nome) {
        return ar.findByNomeIgnoreCase(nome); // precisa mudar o método no repository para retornar AlunoModel
    }

    public Optional<AlunoModel> buscarPorCPF(String cpf) {
        return ar.findByCpf(cpf);
    }

}
