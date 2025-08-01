package rs.russian.portal.user.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import rs.russian.generated.model.ContractDto
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.Contract
import java.util.*

@Mapper(imports = [UUID::class])
interface ContractMapper {

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "account", source = "account")
    fun map(contractDto: ContractDto, account: Account): Contract

    fun map(contract: Contract): ContractDto

    fun map(contracts: List<Contract>): MutableList<ContractDto>
}
