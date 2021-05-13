section.text
    extern write
    extern exit

main:
    global main
    mov rdi, message
    mov rsi, messageLen
    call print
    mov rdi, buf
    mov rsi, 42
    call itoa 
    mov rsi, rax
    call print
    mov rdi, message2
    mov rsi, 1
    call print
    mov rdi, 0
    call exit WRT ..plt
    ret

itoa:
    mov r8, 0		; len
    mov rax, rsi	; tmp
    mov ebx, 10
    loop:
    	inc r8
    	mov edx, 0
    	div ebx
    	cmp rax, 0
    	jg loop
   lea r9, [rdi + r8]
   mov rax, rsi
   loop2:
    	mov edx, 0
    	div ebx
    	add dl, '0'
    	dec r9
    	mov [r9], dl
   		cmp rax, 0
    	jg loop2
   mov rax, r8
   ret  
    
; win | gnu
; rcx - rdi
; rsp - rsp
; rdx - rsi

print: 			; (rdi=message, rsi=messageLen)
    sub rsp, 56
    mov rdx, rsi
    mov rsi, rdi
    mov rdi, 1
    call write WRT ..plt
    add rsp, 56
    ret

section .data
message:
    db  'Hello, World', 10
	messageLen equ $ - message
message2:
    db 10


section .bss
buf:
	resb 20
